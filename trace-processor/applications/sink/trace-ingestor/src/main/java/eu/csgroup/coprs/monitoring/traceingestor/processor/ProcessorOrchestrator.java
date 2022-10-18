package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import eu.csgroup.coprs.monitoring.traceingestor.association.AssociationFactory;
import eu.csgroup.coprs.monitoring.traceingestor.association.DefaultAssociation;
import eu.csgroup.coprs.monitoring.traceingestor.config.DuplicateProcessing;
import eu.csgroup.coprs.monitoring.traceingestor.config.Ingestion;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.InterruptedOperationException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

@Slf4j
@Data
public class ProcessorOrchestrator implements Function<EntityIngestor, List<DefaultEntity>> {
    private Collection<ProcessorDescription> processorDescriptions;

    private BeanAccessor beanAccessor;

    private Ingestion ingestionConfig;

    @Override
    public List<DefaultEntity> apply(EntityIngestor entityIngestor) {
        final var cachedEntities = new HashMap<String, List<DefaultEntity>>();
        final var toProcess = new LinkedList<>(processorDescriptions);

        while(! toProcess.isEmpty()) {
            final var processorDesc = toProcess.poll();
            final var entityMetadata = processorDesc.getEntityMetadata();
            log.debug("Process %s%n".formatted(processorDesc.getName()));

            final var relyOnProc = processorDesc.getRelyOnProc()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .toList();

            if (relyOnProc.isEmpty()
                    || cachedEntities.keySet().containsAll(relyOnProc)) {

                // Map trace to one or more entity
                var processedEntities = createProcessor(processorDesc, entityIngestor).apply(beanAccessor);

                List<DefaultEntity> finalEntities = processedEntities;

                // Process association
                if (! relyOnProc.isEmpty()) {

                    final var nestedEntities = processorDesc.getRelyOnProc()
                            .entrySet()
                            .stream()
                            .map(entry -> Map.entry(
                                    entry.getKey(),
                                    entry.getValue()
                                            .stream()
                                            .flatMap(relyOnProcName -> cachedEntities.get(relyOnProcName).stream())
                                            .collect(ArrayList<DefaultEntity>::new, ArrayList::add, ArrayList::addAll))
                            )
                            .collect(HashMap<Class<? extends DefaultEntity>, List<DefaultEntity>>::new, (h,o) -> h.put(o.getKey(), o.getValue()), HashMap::putAll);

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

                    finalEntities = processedEntities.stream()
                            .flatMap(processedEntity -> associate(
                                    processedEntity,
                                    nestedEntities,
                                    relyOnEntities,
                                    entityIngestor).stream()
                            ).toList();
                }

                cachedEntities.put(processorDesc.getName(), finalEntities);
            } else {
                log.debug("Cannot Process %s (dependency missing)%n".formatted(processorDesc.getEntityMetadata().getEntityName()));
                toProcess.add(processorDesc);
            }
        }

        selectDuplicateStartPoint(beanAccessor).ifPresent(
                config -> setDuplicateProcess(config, entityIngestor, cachedEntities)
        );

        return cachedEntities.values()
                .stream()
                .flatMap(List::stream)
                .toList();
    }


    private List<DefaultEntity> associate(
            DefaultEntity containerEntity,
            Map<Class<? extends DefaultEntity>, List<DefaultEntity>> cachedReferences,
            Map<Class<? extends DefaultEntity>, DefaultAssociation> associationMap,
            EntityFinder entityFinder) {
        List<DefaultEntity> associatedEntities = new ArrayList<>();
        associatedEntities.add(containerEntity);


        for (Map.Entry<Class<? extends DefaultEntity>, DefaultAssociation> currentAssociationEntry : associationMap.entrySet()) {
            List<DefaultEntity> currentReferences = cachedReferences.get(currentAssociationEntry.getKey());

            associatedEntities = associatedEntities.stream()
                    .flatMap(entity -> currentAssociationEntry.getValue().associate(entity, currentReferences, entityFinder)
                            .stream()
                    ).toList();
        }

        return associatedEntities;
    }

    private DefaultProcessor createProcessor(ProcessorDescription processorDesc, EntityFinder entityFinder) {
        return new DefaultProcessor(this.ingestionConfig.getName(), processorDesc, entityFinder);
    }

    private Optional<DuplicateProcessing> selectDuplicateStartPoint (BeanAccessor beanAccessor) {
        if (this.ingestionConfig.getDuplicateProcessings() != null) {
            return this.ingestionConfig
                    .getDuplicateProcessings()
                    .stream()
                    .filter(conf -> conf.test(beanAccessor))
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    private void setDuplicateProcess (
            DuplicateProcessing conf,
            EntityIngestor entityIngestor,
            Map<String, List<DefaultEntity>> processedEntities) {

        var initialQuery = conf.getQuery();
        var inputQuery = initialQuery;

        final var values = new ArrayList<List<Object>>();
        var index = 1;
        if (inputQuery != null) {
            final var matcher = Pattern.compile("<([a-zA-Z0-9_,.]*)>").matcher(inputQuery);
            while(matcher.find()) {
                final var rawPath = matcher.group(1);

                values.add(getValueForDuplicateProcess(initialQuery, matcher.group(0), rawPath, processedEntities));

                inputQuery = inputQuery.replace(matcher.group(0), "?" + index++);
            }

            // Abnormal situation when nothing is retrieved (at least we must have ids of input our output of the processing)
            if (values.isEmpty()) {
                throw new InterruptedOperationException("Duplicate query does not contain any entity to use (%s)".formatted(inputQuery));
            } else {
                entityIngestor.setDuplicateProcessing(inputQuery, values);
            }
        }
    }

    private List<Object> getValueForDuplicateProcess (String query, String capturingGroup, String rawPath, Map<String, List<DefaultEntity>> processedEntities) {
        final var pathes = Arrays.stream(rawPath.split(","))
                .map(PropertyUtil::snake2PascalCasePath)
                .toList();
        List<Object> tempRes;
        if (pathes.size() == 1) {
            tempRes = getValueForDuplicateProcess(pathes.get(0), processedEntities);
        } else {
            tempRes = new ArrayList<>();
            for (var path: pathes) {
                tempRes.addAll(getValueForDuplicateProcess(path, processedEntities));
            }
        }

        return tempRes;
    }

    private List<Object> getValueForDuplicateProcess (final String rawPath, Map<String, List<DefaultEntity>> processedEntities) {
        var separatorIndex = rawPath.indexOf(".");

        final var entityType = separatorIndex != -1 ? rawPath.substring(0, separatorIndex) : rawPath;
            return processedEntities.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().equals(entityType))
                    .map(Map.Entry::getValue)
                    .flatMap(List::stream)
                    .map(entity -> BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(entity)))
                    .map(bean -> bean.getPropertyValue(new BeanProperty(rawPath)))
                    .toList();
    }
}
