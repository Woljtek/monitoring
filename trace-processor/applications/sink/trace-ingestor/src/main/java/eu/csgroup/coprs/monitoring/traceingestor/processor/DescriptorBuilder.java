package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFactory;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityMetadata;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import eu.csgroup.coprs.monitoring.traceingestor.association.AssociationFactory;
import eu.csgroup.coprs.monitoring.traceingestor.association.DefaultAssociation;
import eu.csgroup.coprs.monitoring.traceingestor.config.Alias;
import eu.csgroup.coprs.monitoring.traceingestor.config.Ingestion;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DescriptorBuilder {

    private final Map<String, List<Mapping>> allMappings;

    private final Map<String, Alias> allAlias;

    private final Map<String, ProcessorDescription> cache;

    private final String ingestionConfigName;

    public DescriptorBuilder (Ingestion globalIngestionConfig) {
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
    private ProcessorDescription createDescriptor(String procName, Class<? extends DefaultEntity> entityClass, ProcessorDescription containerProc) {
        if (cache.get(procName) == null) {
            final var entityMetadata = EntityFactory.getInstance().getMetadata(entityClass);

            // Key: entity class
            // Value: associated processor descriptor
            final var reliesOn = entityMetadata.getRelyOn()
                    .keySet()
                    .stream()
                    // Handle polymorphism case
                    .map(relyOn -> getRelyOnChildIfAny (relyOn, procName)
                    ).collect(HashMap<Class<? extends DefaultEntity>, List<Map.Entry<Class<? extends DefaultEntity>, String>>>::new, (l,n) -> l.put(n.getKey(), n.getValue()), HashMap::putAll);

            Ingestion ingestionConfig = getIngestionConfig(procName, entityClass, containerProc, reliesOn);


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
        }

        return cache.get(procName);
    }

    private Map.Entry<Class<? extends DefaultEntity>, List<Map.Entry<Class<? extends DefaultEntity>, String>>> getRelyOnChildIfAny (EntityMetadata metadata, String procName) {
        return metadata.getChild().isEmpty() ?
                Map.entry(
                        metadata.getEntityClass(),
                        List.of(Map.entry(
                                metadata.getEntityClass(),
                                getProcNameFrom(procName, metadata.getEntityClass().getSimpleName())
                        ))
                ) :
                Map.entry(
                        metadata.getEntityClass(),
                        metadata.getChild()
                                .stream()
                                .map(childClass -> Map.<Class<? extends DefaultEntity>, String>entry(
                                        childClass,
                                        getProcNameFrom(procName, childClass.getSimpleName()))
                                ).collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
                );
    }

    private Ingestion getIngestionConfig(String procName, Class<? extends DefaultEntity> entityClass, ProcessorDescription containerProc,
                                         Map<Class<? extends DefaultEntity>, List<Map.Entry<Class<? extends DefaultEntity>, String>>> reliesOn) {
        final var mappings = allMappings.get(procName);
        Ingestion ingestionConfig = null;

        if (mappings != null) {
            ingestionConfig = new Ingestion(
                    ingestionConfigName,
                    mappings,
                    new ArrayList<>(),
                    Map.of()
            );
        } else if (containerProc != null){
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

            if (association.getClass().getSuperclass().equals(DefaultAssociation.class) || Boolean.TRUE.equals(reliesOnComplete)) {
                ingestionConfig = new Ingestion(ingestionConfigName, List.of(), List.of(), Map.of());
            }
        }

        return ingestionConfig;
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
