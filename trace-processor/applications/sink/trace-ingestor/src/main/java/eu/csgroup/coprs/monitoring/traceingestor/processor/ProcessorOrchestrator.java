package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import  eu.csgroup.coprs.monitoring.common.datamodel.entities.MissingProducts;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Processing;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import eu.csgroup.coprs.monitoring.traceingestor.association.AssociationFactory;
import eu.csgroup.coprs.monitoring.traceingestor.association.DefaultAssociation;
import eu.csgroup.coprs.monitoring.traceingestor.config.DuplicateProcessing;
import eu.csgroup.coprs.monitoring.traceingestor.config.Ingestion;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityState;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.InterruptedOperationException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * With the relation set in {@link ProcessorDescription} define in which order to execute them. Entity which are
 * referenced by another entity (container) are processed first then the entity container is processed.<br>
 * <br>
 * When referenced entity (i.e. {@link Processing}) and container entity (i.e. {@link MissingProducts}) are processed
 * association is done (i.e. {@link MissingProducts#setProcessing(Processing)})
 */
@Slf4j
@Data
public class ProcessorOrchestrator implements Function<EntityIngestor, List<DefaultEntity>> {
    private Collection<ProcessorDescription> processorDescriptions;

    private BeanAccessor beanAccessor;

    private Ingestion ingestionConfig;

    @Override
    public List<DefaultEntity> apply(EntityIngestor entityIngestor) {
        // Store processed entities
        final var cachedEntities = new HashMap<String, List<EntityProcessing>>();
        // Order of processed description to execute
        final var toProcess = new LinkedList<>(processorDescriptions);

        while(! toProcess.isEmpty()) {
            // Retrieve first processor description
            final var processorDesc = toProcess.poll();
            final var entityMetadata = processorDesc.getEntityMetadata();
            log.debug("Process %s%n".formatted(processorDesc.getName()));

            final var relyOnProc = processorDesc.getRelyOnProc()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .toList();

            // Execute process if entity does not have relation with other entity
            // Or all referenced entities are already created
            if (relyOnProc.isEmpty()
                    || cachedEntities.keySet().containsAll(relyOnProc)) {

                // Map trace to one or more entity
                // Create processor then execute it
                var processedEntities = createProcessor(processorDesc, entityIngestor).apply(beanAccessor);

                List<EntityProcessing> finalEntities = processedEntities;

                // Process association
                if (! relyOnProc.isEmpty()) {

                    // Retrieve cached entities (processed) based on relation set in processor descriptor
                    final var nestedEntities = processorDesc.getRelyOnProc()
                            .entrySet()
                            .stream()
                            .map(entry -> Map.entry(
                                    entry.getKey(),
                                    entry.getValue()
                                            .stream()
                                            .flatMap(relyOnProcName -> cachedEntities.get(relyOnProcName).stream())
                                            .collect(ArrayList<EntityProcessing>::new, ArrayList::add, ArrayList::addAll))
                            )
                            .collect(HashMap<Class<? extends DefaultEntity>, List<EntityProcessing>>::new, (h,o) -> h.put(o.getKey(), o.getValue()), HashMap::putAll);

                    // For each relation with the current entity, create association instance that must be used to set
                    // referenced entity in container entity (current processed)
                    final var relyOnEntities = entityMetadata.getRelyOn()
                            .entrySet()
                            .stream()
                            .collect(HashMap<Class<? extends DefaultEntity>, DefaultAssociation>::new,
                                    (h,o) -> h.put(
                                            o.getKey().getEntityClass(),
                                            AssociationFactory.getInstance().selectAssociation(
                                                    processorDesc.getEntityMetadata().getEntityClass(),
                                                    o.getKey().getEntityClass(),
                                                    o.getValue())),
                                    HashMap::putAll);

                    // Process all associations
                    finalEntities = processedEntities.stream()
                            .flatMap(processedEntity -> associate(
                                    processedEntity,
                                    nestedEntities,
                                    relyOnEntities,
                                    entityIngestor).stream()
                            ).toList();
                }

                // Finally store processed entities in cache
                cachedEntities.put(processorDesc.getName(), finalEntities);
            } else {
                // All conditions are not met (referenced entities are not created for the given container entity)
                log.debug("Cannot Process %s (dependency missing)%n".formatted(processorDesc.getEntityMetadata().getEntityName()));
                // Set processor description to the end of the list
                toProcess.add(processorDesc);
            }
        }

        // If ingestion config contains duplicate processing sql query, select one that match the trace and execute it
        selectDuplicateStartPoint(beanAccessor).ifPresent(
                config -> setDuplicateProcess(config, entityIngestor, cachedEntities)
        );

        // For processed entities, remove those which were not modified (unchanged state)
        return cachedEntities.values()
                .stream()
                .flatMap(List::stream)
                .filter(entityProcessing -> entityProcessing.getState() != EntityState.UNCHANGED )
                .map(EntityProcessing::getEntity)
                .toList();
    }

    /**
     *  Associate each referenced entity in the container entity by processing copy of the last one when needed
     *
     * @param containerEntity entity in which to set referenced entities
     * @param cachedReferences referenced entities to set in container
     * @param associationMap association instance to use for each referenced entity to set in container entity
     * @param entityFinder interface to access to the storage
     * @return all possible combination with cached references
     */
    private List<EntityProcessing> associate(
            EntityProcessing containerEntity,
            Map<Class<? extends DefaultEntity>, List<EntityProcessing>> cachedReferences,
            Map<Class<? extends DefaultEntity>, DefaultAssociation> associationMap,
            EntityFinder entityFinder) {
        List<EntityProcessing> associatedEntities = new ArrayList<>();
        // Store association result after each loop to reuse it for the next association loop
        associatedEntities.add(containerEntity);


        // For each association
        for (Map.Entry<Class<? extends DefaultEntity>, DefaultAssociation> currentAssociationEntry : associationMap.entrySet()) {
            // Retrieve referenced entities for the given association
            List<EntityProcessing> currentReferences = cachedReferences.get(currentAssociationEntry.getKey());

            // Execute association
            associatedEntities = associatedEntities.stream()
                    .flatMap(entity -> currentAssociationEntry.getValue().associate(entity, currentReferences, entityFinder)
                            .stream()
                    ).toList();
        }

        return associatedEntities;
    }

    private DefaultProcessor createProcessor(ProcessorDescription processorDesc, EntityFinder entityFinder) {
        return new DefaultProcessor(processorDesc, entityFinder);
    }

    /**
     * Select duplicate processing SQL query where rules match to the values of the trace
     *
     * @param beanAccessor bean accessor trace
     * @return duplicate processing SQL query if one found otherwise empty result
     */
    private Optional<DuplicateProcessing> selectDuplicateStartPoint (BeanAccessor beanAccessor) {
        // If ingestion config contains duplicate processing SQL query
        if (this.ingestionConfig.getDuplicateProcessings() != null) {
            // Select the one where rules match values in trace
            return this.ingestionConfig
                    .getDuplicateProcessings()
                    .stream()
                    .filter(conf -> conf.test(beanAccessor))
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    /**
     *  With the given SQL query, replace key with the desired value of processed entities
     *
     * @param conf SQL query and rules
     * @param entityIngestor interface with the storage
     * @param processedEntities Processed entities to use to replace key in SQL query with desired value
     */
    private void setDuplicateProcess (
            DuplicateProcessing conf,
            EntityIngestor entityIngestor,
            Map<String, List<EntityProcessing>> processedEntities) {

        var initialQuery = conf.getQuery();
        log.debug("Apply duplicate process (query %s)".formatted(conf.getQuery()));

        var inputQuery = initialQuery;

        if (inputQuery != null) {
            final var values = new ArrayList<List<Object>>();
            var index = 1;
            // Find key to replace with field value of processed entities (must be of the form <dsib.id>)
            final var matcher = Pattern.compile("<([a-zA-Z0-9_,.]*)>").matcher(inputQuery);
            final var debugParameter = new StringBuilder("Query parameters: ");

            while(matcher.find()) {
                // Get raw result (without <...>)
                final var rawPath = matcher.group(1);

                // Find the desired value in the processed entities
                final var value = getValueForDuplicateProcess(initialQuery, matcher.group(0), rawPath, processedEntities);
                if (value.isEmpty()) {
                    log.warn("Cancel duplicate processing because all conditions are not met (no value found or null value for %s)".formatted(rawPath));
                    return;
                }
                values.add(value);

                // Replace key with a placeholder
                inputQuery = inputQuery.replace(matcher.group(0), "?" + index++);

                debugParameter.append(rawPath).append(" => ").append(values.get(values.size() - 1)).append(";");
            }
            log.debug(debugParameter.toString());

            // Abnormal situation when nothing is retrieved (at least we must have ids of input our output of the processing)
            if (values.isEmpty()) {
                throw new InterruptedOperationException("Duplicate query does not contain any entity to use (%s)".formatted(inputQuery));
            } else {
                entityIngestor.setDuplicateProcessing(inputQuery, values);
            }
        }
    }

    /**
     * Retrieve values contained in processed entities according to paths set
     *
     * @param query SQL query containing key
     * @param capturingGroup raw path with <...>
     * @param rawPath bean property path (can be multiple path separated by a coma ',')
     * @param processedEntities Entities to use to retrieve values
     * @return
     */
    private List<Object> getValueForDuplicateProcess (String query, String capturingGroup, String rawPath, Map<String, List<EntityProcessing>> processedEntities) {
        // Check if it's a multiple path
        final var pathes = Arrays.stream(rawPath.split(","))
                .map(PropertyUtil::snake2PascalCasePath)
                .toList();
        List<Object> tempRes;
        // If not retrieve values for the single path
        if (pathes.size() == 1) {
            tempRes = getValueForDuplicateProcess(pathes.get(0), processedEntities);
        } else {
            // Otherwise retrieve values for each path
            tempRes = new ArrayList<>();
            for (var path: pathes) {
                tempRes.addAll(getValueForDuplicateProcess(path, processedEntities));
            }
        }

        return tempRes;
    }

    /**
     * Retrieve value of each processed entities that match the entity type set in path
     *
     * @param rawPath bean property path
     * @param processedEntities processed entities
     * @return values associated to given path for each processed entities where entity type is the one set in path
     */
    private List<Object> getValueForDuplicateProcess (final String rawPath, Map<String, List<EntityProcessing>> processedEntities) {
        var separatorIndex = rawPath.indexOf(".");

        final var entityType = separatorIndex != -1 ? rawPath.substring(0, separatorIndex) : rawPath;
        
        return processedEntities.entrySet()
                .stream()
                // Find processed entities which match the desired entity type
                .filter(entry -> entry.getKey().equals(entityType))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                // Access the value in the entity
                .map(entityProcessing -> BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(entityProcessing.getEntity())))
                .map(bean -> bean.getPropertyValue(new BeanProperty(rawPath)))
                .filter(Objects::nonNull)
                .toList();
    }
}