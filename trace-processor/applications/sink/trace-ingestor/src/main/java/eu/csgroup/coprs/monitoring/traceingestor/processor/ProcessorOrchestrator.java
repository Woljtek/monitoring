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
        return new DefaultProcessor(processorDesc, entityFinder);
    }
}
