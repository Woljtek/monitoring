package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
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

/**
 * Build a set of {@link ProcessorDescription} from a given {@link Ingestion} config. The ingestion config contains
 * {@link Mapping} which define with the 'to' clause the entity type to create.<br>
 * <br>
 * For all the defined entity type to create, builder will check relation with the others. If the defined entity type
 * rely on another, the builder will create an extra {@link ProcessorDescription} to avoid generation error (for instance
 * if {@link Chunk} is defined in the configuration but not the
 * {@link Dsib}, builder will find a relation between the two
 * {@link Chunk#setDsib(Dsib)} and so create a {@link ProcessorDescription}
 * for {@link Chunk} entity type and an extra {@link ProcessorDescription}
 * for {@link Dsib} entity type).<br>
 * <br>
 * The same principle as above is applied when a defined entity is referenced by another. Builder will create an extra
 * {@link ProcessorDescription} for the entity type that refers (rely on) to the defined one. For example if
 * {@link Processing} entity is defined in the configuration, builder will create an extra {@link ProcessorDescription} for
 * {@link InputListExternal} (see {@link InputListExternal#setId(InputListExternalId)}, {@link InputListExternalId#setProcessing(Processing)})
 * and {@link MissingProducts} (see {@link MissingProducts#setProcessing(Processing)}) entity type.<br>
 * <br>
 * Alias come with the possibility to break relation between two entity type (has shown above) with the use of 'restrict'
 * clause. When builder come to such a case, extra {@link ProcessorDescription} won't be created. This can be used with the case
 * of {@link Product} which is referenced by {@link InputListInternal} and {@link OutputList} to select for which
 * relation a product is created.
 *
 */
public class DescriptorBuilder {

    /**
     * Mapping configuration grouped by entity type/alias
     */
    private final Map<String, List<Mapping>> allMappings;

    private final Map<String, Alias> allAlias;

    private final String configurationName;

    /**
     * Store created descriptor per entity type/alias to avoid duplicate descriptor on the same entity type/alias when
     * dealing with entity relation
     */
    private final Map<String, ProcessorDescription> cache;


    /**
     * Set ingestion configuration to analyze in order to identify entity type to process.
     *
     * @param globalIngestionConfig ingestion configuration to analyze.
     */
    public DescriptorBuilder (Ingestion globalIngestionConfig) {
        configurationName = globalIngestionConfig.getName();
        //Split configuration per entity type
        allMappings = globalIngestionConfig.getMappings()
                .stream()
                .collect(Collectors.groupingBy(m -> m.getTo().getBeanName()));

        // Set alias associated to the configuration.
        allAlias = globalIngestionConfig.getAlias();

        this.cache = new HashMap<>();
    }

    public Collection<ProcessorDescription> build() {
        // Create description of a process for each entity type declared in the configuration
        // Entity type name is used as the processor description name.
        for(String procName: allMappings.keySet()) {
            createDescriptor(procName);
        }

        return cache.values();
    }

    private void createDescriptor (String procName) {
        // Check if entity type name used as processor description is not an alias instead.
        final var association = getAlias(procName);

        // If it's an alias find the real entity type name.
        // Or use the processor description name as the entity type name (default case)
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
        // Check that the processor description was not already create for the given entity type
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

            final var mappings = getMappings(procName, entityClass, containerProc, reliesOn);

            // Way to determine if all conditions are met to create entity
            // (Dirty way to use ingestion config as indicator)
            if (mappings == null) {
                return null;
            }

            final var processDesc = new ProcessorDescription();
            processDesc.setName(procName);
            processDesc.setConfigurationName(configurationName);
            processDesc.setMappings(mappings);
            processDesc.setEntityMetadata(entityMetadata);

            // For all relation with another entity type
            reliesOn.entrySet()
                    .stream()
                    .map(relyOnEntry -> Map.entry(
                            relyOnEntry.getKey(),
                            relyOnEntry.getValue()
                                    .stream()
                                    // Create associated descriptor if possible
                                    .map(entry -> createDescriptor(
                                            entry.getValue(),
                                            entry.getKey(),
                                            processDesc)
                                    )
                                    .filter(Objects::nonNull)
                                    // When processor description is created keep only processor description name
                                    // (entity type name or alias) for later (identify on which processor description
                                    // the current one rely on)
                                    .map(ProcessorDescription::getName)
                                    .toList())
                    )
                    .forEach(relyOnProcDescEntry -> processDesc.putRelyOnProcs(relyOnProcDescEntry.getKey(), relyOnProcDescEntry.getValue()));

            // All relations have an associated processor description so the current one is valid for processing
            // (able to put in cache)
            if (reliesOn.size() == processDesc.getRelyOnProc().size()) {
                cache.put(procName, processDesc);
            }

            // Get entity type of the container (for example chunk container when dealing with dsib entity)
            final var containerEntityClass = Optional.ofNullable(containerProc)
                    .map(ProcessorDescription::getEntityMetadata)
                    .map(EntityMetadata::getEntityClass)
                    .orElse(null);

            // Add additional processor to process not declared entity in mappings configuration but required
            // by the one declared in mapping configuration.
            processDesc.getEntityMetadata()
                    // For instance dsib is referenced by chunk (dsib field in chunk)
                    .getReferencedBy()
                    .stream()
                    // Do not keep class referencing the current entity if it's the one which is calling method creation
                    .filter(classRef -> !classRef.equals(containerEntityClass))
                    .forEach(foundClass -> createDescriptor(
                            getProcNameFrom(procName, foundClass.getSimpleName()),
                            foundClass,
                            processDesc)
                    );
            // Case of chunk which extends externalInput entity type
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

    /**
     * Utility function to return child entity (if any) of the given entity or the given entity (for example child
     * entity of {@link ExternalInput} are {@link Chunk}, {@link Dsib}, {@link AuxData}).
     *
     * @param metadata Metadata of the entity which contains list of childs
     * @param procName
     * @return
     */
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

    /**
     * Get the mapping configuration associated to the given processor description name (can be an entity type name or
     * an alias).<br>
     * <br>
     * Mapping can come from configuration or computed (empty) when not defined in configuration only if:
     * <ul>
     *     <li>all relations with other entities are satisfied (that's to say processor description are already created
     *     and set in cache)</li>
     *     <li>entities relation is done with a dedicated association instance as of chunk with dsib</li>
     * </ul>
     *
     * @param procName Processor description name (entity type name or alias)
     * @param entityClass Entity type
     * @param containerProc processor description (if any) of the entity that contain the given entity (for instance
     *                      chunk contains a dsib)
     * @param reliesOn all relation with the given entity type
     * @return mapping configuration of the entity or null if all relation are not satisfied or not dedicated association
     * instance is used
     */
    private List<Mapping> getMappings(String procName, Class<? extends DefaultEntity> entityClass, ProcessorDescription containerProc,
                                         Map<Class<? extends DefaultEntity>, List<Map.Entry<Class<? extends DefaultEntity>, String>>> reliesOn) {
        // If the result is null it implies that the processor description name (entity type/alias) is not in configuration
        // but is a relation with another entity type/alias
        var mappings = allMappings.get(procName);

        // Condition that describe a relation with another entity type/alias
        if (mappings == null && containerProc != null) {
            // Find between the two association instance to use (default or specific one)
            final var association = AssociationFactory.getInstance().selectAssociation(
                    containerProc.getEntityMetadata().getEntityClass(),
                    entityClass,
                    null);

            // Check that all relation with entities type are satisfied (processor description for each entity are in cache)
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

            // For entity type not defined in configuration create an empty mapping when
            // all relation are satisfied
            // or a dedicated association instance is used (case of chunk with dsib)
            if (association.getClass().getSuperclass().equals(DefaultAssociation.class) || Boolean.TRUE.equals(reliesOnComplete)) {
                mappings = List.of();
            }
        }

        return mappings;
    }

    /**
     * Find the first {@link Alias} instance of the given processor description name if it's an alias otherwise
     * return an empty result
     *
     * @param procName Processor description name
     * @return First {@link Alias} instance found otherwise empty result.
     */
    private Optional<Alias> getAlias(String procName) {
        return allAlias.entrySet()
                .stream()
                .filter(entry -> procName.equals(PropertyUtil.snake2PascalCasePropertyName(entry.getKey())))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    /**
     * Find all alias associated to given entity.
     *
     * @param entityName entity name
     * @return Entity alias or empty stream
     */
    private Stream<String> findEntityAliasName (String entityName) {
        return findEntityAlias(entityName).map(Map.Entry::getKey);
    }

    /**
     * Find all alias associated to the given entity type name
     *
     * @param entityName entity type name to find for an alias
     * @return Entity alias or empty stream
     */
    private Stream<Map.Entry<String, Alias>> findEntityAlias (String entityName) {
        return allAlias.entrySet()
                .stream()
                // Check the correspondence in pascal case
                .filter(entry -> entityName.equals(
                                PropertyUtil.snake2PascalCasePropertyName(
                                        entry.getValue().getEntity())
                        )
                ).map(entry -> Map.entry(
                        PropertyUtil.snake2PascalCasePropertyName(entry.getKey()),
                        entry.getValue()
                ));
    }

    /**
     * Retrieve the processor description name that will be associated to the given entity type.<br>
     * <br>
     * If one is defined use the alias name as the processor description name only if relation between given entity type
     * name and container processor description name is allowed otherwise use the given entity type name as the processor
     * description name.<br>
     * <br>
     * The function privilege the use of alias name (if any) for the container processor description name and also given
     * entity type name instead of entity type name.
     *
     * @param containerProcName Processor description name of the container (for example {@link Chunk} is a container
     *                          regarding {@link Dsib} entity
     * @param entityName Name of the entity for which to define the processor description name
     * @return Processor description name.
     */
    private String getProcNameFrom (String containerProcName, String entityName) {
        // Check if the processor description name is an alias.
        final var containerAlias = allAlias.get(containerProcName);

        if (containerAlias != null) {
            // If so
            // Get all alias associated to the given entity type name
            return findEntityAliasName(entityName)
                    // Keep only one where relation with the container alias name is allowed (cf. 'retrict' clause)
                    .filter(alias -> alias.equals(containerAlias.getRestrict()))
                    .findFirst()
                    // If one alias for the given entity type name is found transform the alias name and use it as
                    // processor description name
                    .map(PropertyUtil::snake2PascalCasePropertyName)
                    // Otherwise use the entity type name as the processor description name
                    .orElse(entityName);
        } else {
            // Otherwise
            // Get all alias associated to the given entity type name
            return findEntityAlias(entityName)
                    // Keep only one which has no restriction or allow relation with the container processor name
                    // (cf. 'retrict' clause) which is here the entity type name
                    .filter(alias -> alias.getValue().getRestrict() == null
                            || PropertyUtil.snake2PascalCasePropertyName(
                            alias.getValue().getRestrict()
                    ).equals(containerProcName))
                    // If one is found use the alias name as the processor description name
                    .map(Map.Entry::getKey)
                    .findFirst()
                    // Otherwise use the entity type name as the processor description name
                    .orElse(entityName);
        }
    }
}
