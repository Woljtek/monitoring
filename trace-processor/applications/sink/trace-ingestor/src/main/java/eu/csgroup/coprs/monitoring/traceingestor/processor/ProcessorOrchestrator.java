package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.traceingestor.association.AssociationFactory;
import eu.csgroup.coprs.monitoring.traceingestor.association.DefaultAssociation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Data
public class ProcessorOrchestrator implements Function<EntityIngestor, List<DefaultEntity>> {
    private Collection<ProcessorDescription> processorDescriptions;

    private BeanAccessor beanAccessor;

    @Override
    public List<DefaultEntity> apply(EntityIngestor entityIngestor) {
        final var cachedEntities = new HashMap<String, List<DefaultEntity>>();
        final var toProcess = new LinkedList<>(processorDescriptions);

        while(! toProcess.isEmpty()) {
            final var processorDesc = toProcess.poll();
            final var entityMetadata = processorDesc.getEntityMetadata();
            log.debug("Process %s\n".formatted(processorDesc.getName()));

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
                                            .toList())
                            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    final var relyOnEntities = entityMetadata.getRelyOn()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey().getEntityClass(),
                                    entry -> AssociationFactory.getInstance().selectAssociation(
                                            processorDesc.getEntityMetadata().getEntityClass(),
                                            entry.getKey().getEntityClass(),
                                            entry.getValue())
                            ));

                    finalEntities = processedEntities.stream()
                            .flatMap(processedEntity -> associate(
                                    processedEntity,
                                    nestedEntities,
                                    relyOnEntities,
                                    entityIngestor).stream()
                            ).toList();
                }

                // If the entity is referenced by more than 1 entity
                // process save operation to ensure entity uniqueness in database.
                // If the entity is single process save operation directly
                // /!\ Not needed JPA will handle this case for use (update reference with id)
                /*if (entityMetadata.getReferencedBy().size() > 1) {
                    finalEntities = entityIngestor.saveAll(finalEntities);
                }*/

                cachedEntities.put(processorDesc.getName(), finalEntities);
            } else {
                System.out.printf("Cannot Process %s (dependency missing)%n", processorDesc.getEntityMetadata().getEntityName());
                toProcess.add(processorDesc);
            }
        }

        return cachedEntities.values()
                .stream()
                .flatMap(List::stream)
                .toList();
    }


    private List<DefaultEntity> associate(
            DefaultEntity containerEntity,
            Map<Class, List<DefaultEntity>> cachedReferences,
            Map<Class, DefaultAssociation> associationMap,
            EntityFinder entityFinder) {
        var associatedEntities = List.of(containerEntity);

        final var associationEntryIt = associationMap.entrySet().iterator();
        while (associationEntryIt.hasNext()) {
            var currentAssociationEntry = associationEntryIt.next();
            var currentReferences = cachedReferences.get(currentAssociationEntry.getKey());

            associatedEntities = associatedEntities.stream()
                    .flatMap(entity -> currentAssociationEntry.getValue().associate(entity, currentReferences, entityFinder)
                            .stream()
                    ).toList();
        }

        return associatedEntities;
    }

    private <T extends DefaultEntity> DefaultProcessor<T> createProcessor(ProcessorDescription processorDesc, EntityFinder entityFinder) {
        /*try {
            final var className = Class.forName("%s.%sProcessor".formatted(DefaultProcessor.class.getPackageName(), processorDesc.getEntityName()));
            return (DefaultProcessor<T>) className.getConstructor(String.class, Ingestion.class, EntityFinder.class)
                    .newInstance(processorDesc, entityFinder);
        } catch (Exception e) {*/
            return new DefaultProcessor<T>(processorDesc, entityFinder);
        //}
    }
}
