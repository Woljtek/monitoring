package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.TraceLog;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFactory;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityMetadata;
import eu.csgroup.coprs.monitoring.traceingestor.association.AssociationFactory;
import eu.csgroup.coprs.monitoring.traceingestor.association.DefaultAssociation;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Alias;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Ingestion;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.IngestionGroup;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.processor.ProcessorOrchestrator;
import eu.csgroup.coprs.monitoring.traceingestor.processor.ProcessorDescription;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

@Slf4j
public class TraceIngestorSink implements Consumer<Message<FilteredTrace>> {

    private final ReloadableBeanFactory reloadableBeanFactory;

    private final EntityIngestor entityIngestor;


    public TraceIngestorSink(ReloadableBeanFactory reloadableBeanFactory, EntityIngestor entityIngestor) {
        this.reloadableBeanFactory = reloadableBeanFactory;
        this.entityIngestor = entityIngestor;
    }

    @Override
    public void accept(Message<FilteredTrace> message) {
        final Instant start = Instant.now();

        final var filteredTrace = message.getPayload();
        // Find mapping associated to filter name
        final var ingestionStrategy = reloadableBeanFactory.getBean(IngestionGroup.class).getIngestions()
                .stream()
                .filter(m -> m.getName().equals(filteredTrace.getRuleName()))
                .findFirst();

        ingestionStrategy.ifPresentOrElse(
                m -> ingest(filteredTrace.getLog(), m),
                () -> {
                    String errorMessage = "No configuration found for '%s'\n%s".formatted(filteredTrace.getRuleName(), filteredTrace.getLog());
                    log.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
        );

        final var duration = Duration.between(start, Instant.now());
        try (MDC.MDCCloseable ignored = MDC.putCloseable("log_param", ",\"ingestion_duration_in_ms\":%s".formatted(duration.toMillis()))) {
            log.info("Trace ingestion with configuration '%s' done (took %s ms)\n%s".formatted(
                    ingestionStrategy.get().getName(),
                    duration.toMillis(),
                    filteredTrace.getLog()));
        }
    }

    protected final <T extends ExternalInput> void ingest(TraceLog traceLog, Ingestion mapping) {
        // Create entity instance from trace instance based on mapping rules.
        try {
            final var beanAccessor = BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(traceLog));

            final var processDescs = new DescriptorBuilder(mapping).build();

            final var orchestrator = new ProcessorOrchestrator();
            orchestrator.setBeanAccessor(beanAccessor);
            orchestrator.setProcessorDescriptions(processDescs);

            entityIngestor.process(orchestrator);
        } catch (Exception e) {
            final var errorMessage = "Error occurred ingesting trace with configuration '%s'\n%s: ".formatted(mapping.getName(), traceLog);
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    private static class DescriptorBuilder {

        /*private final Map<String, List<BeanProperty>> allDependencies;*/

        private final Map<String, List<Mapping>> allMappings;

        private final Map<String, Alias> allAlias;

        private final Map<String, ProcessorDescription> cache;

        private final String ingestionConfigName;

        public DescriptorBuilder (Ingestion globalIngestionConfig) {
            /*allDependencies = globalIngestionConfig.getDependencies()
                    .stream()
                    .collect(Collectors.groupingBy(BeanProperty::getBeanName));*/

            allMappings = globalIngestionConfig.getMappings()
                    .stream()
                    .collect(Collectors.groupingBy(m -> m.getTo().getBeanName()));

            allAlias = globalIngestionConfig.getAlias();

            this.ingestionConfigName = globalIngestionConfig.getName();
            this.cache = new HashMap<>();
        }

        public Collection<ProcessorDescription> build() {
            for(String procName: allMappings.keySet()) {
                createDescriptor(procName);
            }

            return cache.values();
        }

        private void createDescriptor (String procName) {
            final var association = getAlias(procName);

            final var entityName = association.map(Alias::getEntity)
                    .map(PropertyUtil::snake2PascalCasePropertyName)
                    .orElse(procName);

            createDescriptor(procName, EntityHelper.getEntityClass(entityName), null);
        }

        /**
         *
         * @param procName processor name
         * @param entityClass entity for which to create processor description
         * @param containerProc Processor description of the container. A container is identified as an entity 'containing' other entity
         * @return Processor description that will handle creation of desired entity
         */
        private ProcessorDescription createDescriptor(String procName, Class entityClass, ProcessorDescription containerProc) {
            if (cache.get(procName) == null) {
                final var entityMetadata = EntityFactory.getInstance().getMetadata(entityClass);
                final var mappings = allMappings.get(procName);

                // Key: entity class
                // Value: associated processor descriptor
                final var reliesOn = entityMetadata.getRelyOn()
                        .keySet()
                        .stream()
                        // Handle polymorphism case
                        .map(relyOn -> relyOn.getChild().isEmpty() ?
                                Map.entry(
                                        relyOn.getEntityClass(),
                                        List.of(Map.entry(
                                                relyOn.getEntityClass(),
                                                getProcNameFrom(procName, relyOn.getEntityClass().getSimpleName())
                                        ))
                                ) :
                                Map.entry(
                                        relyOn.getEntityClass(),
                                        relyOn.getChild()
                                                .stream()
                                                .map(childClass -> Map.entry(
                                                        childClass,
                                                        getProcNameFrom(procName, childClass.getSimpleName()))
                                                ).toList()
                                )
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Ingestion ingestionConfig = null;
                if (mappings != null) {
                    ingestionConfig = new Ingestion(
                            ingestionConfigName,
                            mappings,
                            new ArrayList<>(),
                            Map.of()
                    );
                } else {
                    final var association = AssociationFactory.getInstance().selectAssociation(
                            containerProc.getEntityMetadata().getEntityClass(),
                            entityClass,
                            null);

                    final var reliesOnComplete = reliesOn.values()
                            .stream()
                            .map(relyOnList -> relyOnList
                                    .stream()
                                    .map(entry -> cache.containsKey(entry.getValue()) || containerProc.getName().equals(entry.getValue()))
                                    // Check that at least one child entity will be processed
                                    .reduce(false, (l,n) -> l || n)
                            )
                            // Check that all rely on entity as at least one entity that will be processed
                            .reduce(true, (l,n) -> l && n);

                    if (association.getClass().getSuperclass().equals(DefaultAssociation.class) || reliesOnComplete) {
                        ingestionConfig = new Ingestion(ingestionConfigName, List.of(), List.of(), Map.of());
                    }
                }

                // Way to determine if all conditions are met to create entity
                // (Dirty way to use ingestion config as indicator)
                if (ingestionConfig == null) {
                    return null;
                }

                final var processDesc = new ProcessorDescription();
                processDesc.setName(procName);
                processDesc.setIngestionConfig(ingestionConfig);
                processDesc.setEntityMetadata(entityMetadata);

                reliesOn.entrySet()
                        .stream()
                        .map(relyOnEntry -> Map.entry(
                                relyOnEntry.getKey(),
                                relyOnEntry.getValue()
                                        .stream()
                                        .map(entry -> createDescriptor(
                                                entry.getValue(),
                                                entry.getKey(),
                                                processDesc)
                                        )
                                        .filter(Objects::nonNull)
                                        .map(ProcessorDescription::getName)
                                        .toList())
                        )
                        .forEach(relyOnProcDescEntry -> processDesc.putRelyOnProcs(relyOnProcDescEntry.getKey(), relyOnProcDescEntry.getValue()));

                if (reliesOn.size() == processDesc.getRelyOnProc().size()) {
                    cache.put(procName, processDesc);
                }

                final var containerEntityClass = Optional.ofNullable(containerProc)
                        .map(ProcessorDescription::getEntityMetadata)
                        .map(EntityMetadata::getEntityClass)
                        .orElse(null);

                // Add additional processor to process not declared entity in mappings configuration but required
                // by the one declared in mapping configuration.
                processDesc.getEntityMetadata()
                        .getReferencedBy()
                       .stream()
                        // Do not keep class referencing the current entity if it's the one which is calling method creation
                        .filter(classRef -> !classRef.equals(containerEntityClass))
                        .forEach(foundClass -> createDescriptor(
                                getProcNameFrom(procName, foundClass.getSimpleName()),
                                foundClass,
                                processDesc)
                        );
               Optional.ofNullable(
                        EntityFactory.getInstance()
                                .getMetadata(entityClass.getSuperclass())
                       ).stream()
                       .flatMap(parentMetadata -> parentMetadata.getReferencedBy().stream())
                       .forEach(referencedByParentMetadata -> createDescriptor(
                               getProcNameFrom(procName, referencedByParentMetadata.getSimpleName()),
                               referencedByParentMetadata,
                               processDesc));
                       /*.filter(Objects::nonNull)
                       .forEach(referenceByProcDesc -> {
                           var relyOnProcNameList = processDesc.getRelyOnProc().computeIfAbsent(
                                   entityClass.getSuperclass(), k -> new ArrayList<>());
                           relyOnProcNameList.add(referenceByProcDesc.getName());
                       })*/;
            }

            return cache.get(procName);
        }

        private Optional<Alias> getAlias(String procName) {
            return allAlias.entrySet()
                    .stream()
                    .filter(entry -> procName.equals(PropertyUtil.snake2PascalCasePropertyName(entry.getKey())))
                    .findFirst()
                    .map(Map.Entry::getValue);
        }

        /**
         * Find all alias associated to given entity. If no alias found
         * return an empty list
         *
         * @param entityName entity name
         * @return Entity alias or empty
         */
        private Stream<String> findEntityAliasName (String entityName) {
            return findEntityAlias(entityName).map(Map.Entry::getKey);
        }

        private Stream<Map.Entry<String, Alias>> findEntityAlias (String entityName) {
            return allAlias.entrySet()
                    .stream()
                    .filter(entry -> entityName.equals(
                                    PropertyUtil.snake2PascalCasePropertyName(
                                            entry.getValue().getEntity())
                            )
                    ).map(entry -> Map.entry(
                            PropertyUtil.snake2PascalCasePropertyName(entry.getKey()),
                            entry.getValue()
                    ));
        }

        private String getProcNameFrom (String containerProcName, String entityName) {
            final var containerAlias = allAlias.get(containerProcName);

            if (containerAlias != null) {
                return findEntityAliasName(entityName).filter(alias -> alias.equals(containerAlias.getRestrict()))
                        .findFirst()
                        .map(PropertyUtil::snake2PascalCasePropertyName)
                        .orElse(entityName);
            } else {
                return findEntityAlias(entityName).filter(alias -> alias.getValue().getRestrict() == null
                            || PropertyUtil.snake2PascalCasePropertyName(
                                    alias.getValue().getRestrict()
                                ).equals(containerProcName))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(entityName);
            }
        }
    }
}
