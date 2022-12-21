package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityState;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.Parser;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.TraceMapper;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.TreePropertyLeaf;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.TreePropertyNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Process creation of entities for a specific entity type defined in {@link ProcessorDescription}.<br>
 * <br>
 * The process is based on four phases:
 * <ul>
 *     <li>Extract values in tree structure form to use to fill entities</li>
 *     <li>Identify which entity to fill (empty or stored)</li>
 *     <li>Fill entities with extracted values (mapping phase)</li>
 *     <li>Check entities consistency (no null value for field annotated as non null and no duplicate entities where
 *     field value must be unique)</li>
 * </ul>
 *
 * First compute the tree structure associated to the given bean (see {@link eu.csgroup.coprs.monitoring.common.datamodel.Trace})
 * for mappings defined in {@link ProcessorDescription} by using the 'from' clause which are associated to the trace<br>
 * <br>
 * From the computed tree structure extract leafs which are associated to field where value must be unique
 * (for example {@link Dsib#getFilename()} which has an {@link Column} annotation with unique attribute set to true)
 * With those leaf identifies entities that must be created.<br>
 * On one hand find and retrieve entities in storage that match the ones that must be created and on the other hand
 * create default entities that were not found in storage be setting uniqueness values.<br>
 * <br>
 * With entities retrieved from storage and those created, fill entities with leaf values that were not extracted in
 * the above step.<br>
 * <br>
 * The last step is a check process which ensure that entities are well-formed that's to say no duplicate entities
 * (where field annotated with unique attribute is set to true) and no entities with null values (where field annotated
 * with non null attribute is set to true)
 *
 * @param processorDesc description of the entity type to create and the mapping to use to fill entities
 * @param entityFinder Instance to find stored entities
 */
@Slf4j
public record DefaultProcessor(
        ProcessorDescription processorDesc,
        EntityFinder entityFinder) implements Function<BeanAccessor, List<EntityProcessing>> {
    /**
     * Construct {@link Specification} to process search in storage from a set of given values.<br>
     * Value is associated to its mapping to determine which field to use in the specification.
     *
     * @param dependenciesValue map of value associated to it's mapping (nested to extract the 'to' path which designate
     *                         a field of the entity)
     * @return specification constructed from value and the associated field (mapping)
     * @param <T> Type of the entity to look for in storage.
     */
    private <T extends DefaultEntity> Specification<T> getFindClauses(Map<Mapping, Object> dependenciesValue) {
        // Feature: Handle arrays equality case
        return dependenciesValue.entrySet().stream()
                .map(entry -> EntitySpecification.<T>getEntityBy(
                        entry.getKey().getTo().getRawBeanPropertyPath(),
                        entry.getValue())
                ).reduce(Specification.where(null), Specification::and);
    }

    /**
     * From the {@link eu.csgroup.coprs.monitoring.common.ingestor.EntityMetadata} contained in {@link ProcessorDescription}
     * identify field of the entity where value must be unique to extract in the set of mapping those which are associated
     * to the desired field.
     *
     * @return Set of mapping which are associated to a field where value must be unique.
     */
    private List<Mapping> getBeanPropertyDependencies() {
        return processorDesc.getEntityMetadata()
                .getDependencies()
                .stream()
                .map(field -> processorDesc.getMappings()
                        .stream()
                        // The 'to' clause (path) is associated to a field of the mapping
                        // So check if it matches to the desired one.
                        .filter(mapping -> mapping.getTo().getBeanPropertyPath().equals(field.getName()))
                        .findFirst()
                        .orElse(null)
                ).filter(Objects::nonNull)
                .toList();
    }

    /**
     * Extract all leaf of the tree whatever position in.
     *
     * @param node Node from which to extract leafs.
     * @return all leaf of the tree.
     */
    private Stream<TreePropertyLeaf> extractLeaves (TreePropertyNode node) {
        return Stream.concat(
                node.getLeaves().stream(),
                node.getNodes()
                        .stream()
                        .flatMap(this::extractLeaves)
        );
    }

    /**
     * Construct the 'to' clause value from a set of 'from' clause value (reduce operation) for each leaf given.
     * The value is constructed with the help of {@link TraceMapper#mapPropertyValue(Mapping, Map)}
     *
     * @param leaves Set of leaf to construct 'to' clause value
     * @return a list of value if the is more than one leaf given otherwise a single value or a collection depending on
     * the result of the operation
     */
    private Object reducePropertyValues(List<TreePropertyLeaf> leaves) {
        if (leaves.size() == 1) {
            final var leaf = leaves.get(0);
            return TraceMapper.mapPropertyValue(leaf.getRule(), leaf.getRawValues());
        } else {
            return leaves.stream()
                    .map(leaf -> TraceMapper.mapPropertyValue(leaf.getRule(), leaf.getRawValues()))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    /**
     * Find among available entities list the one that match the required entity by extracting values of the last one
     * according to given mapping list (use 'to' clause to extract value of the required entity)
     *
     * @param requiredEntity the entity to find among given available entity list
     * @param beanPropDep List of mapping to use to find the entity
     * @param availableEntityValues list of entity to find in
     * @return Found entity or empty
     */
    private Optional<EntityProcessing> getAvailableEntity(EntityProcessing requiredEntity, List<Mapping> beanPropDep, Map<List<Object>, EntityProcessing> availableEntityValues) {
        // For the required entity extract value for each mapping
        final var requiredEntityValues = beanPropDep.stream()
                .map(rule -> requiredEntity.getPropertyValue(rule.getTo()))
                .toList();

        // Then find entity in available entities list the one which has the same extracted value.
        return Optional.ofNullable(availableEntityValues.get(requiredEntityValues));
    }

    @Override
    public List<EntityProcessing> apply(BeanAccessor beanAccessor) {
        final var handler = new DefaultHandler(processorDesc.getEntityMetadata().getEntityClass());
        final var mapper = new TraceMapper(beanAccessor, processorDesc().getConfigurationName());
        // Create tree structure associated to extracted value from given mapping list (node is an index of a list)
        final var treePropertyValue = new Parser(processorDesc.getMappings()).parse(beanAccessor);

        final var availableEntities = new ArrayList<EntityProcessing>();


        // Retrieve mapping that refer to an entity field marked as unique.
        final var beanPropDep = getBeanPropertyDependencies();

        if (!beanPropDep.isEmpty()) {
            // Compute value for unique entity field
            // Can be single value or array
            final var dependencyValue = extractLeaves(treePropertyValue)
                    // Find leaf which are associated to a field where value must be unique.
                    .filter(leaf -> beanPropDep.contains(leaf.getRule()))
                    // Group values associated to same mapping (case of values dispatched over different index of a list)
                    .collect(Collectors.groupingBy(TreePropertyLeaf::getRule))
                    .entrySet()
                    .stream()
                    // Get the final value
                    .collect(Collectors
                            .toMap(Map.Entry::getKey, entry -> reducePropertyValues(entry.getValue()))
                    );

            // When entity has unique field find them in storage with above calculated values
            if (dependencyValue.size() > 0) {
                availableEntities.addAll(
                        // Format value into a specification before sending request.
                        entityFinder.findAll(getFindClauses(dependencyValue), handler.getEntityClass())
                                // wrap received entities in EntityProcessing
                                .stream()
                                .map(entity -> EntityProcessing.fromEntity(entity, EntityState.UNCHANGED))
                                .toList()

                );
            }

            // Create default entities and fill field for unique value.
            final var requiredEntities = mapper.map(beanPropDep, handler);

            log.debug("Number of available entities %s".formatted(availableEntities.size()));
            log.debug("Number of required entities %s".formatted(requiredEntities.size()));

            if (availableEntities.isEmpty()) {
                // No entities is storage use created entities as default entities
                handler.setDefaultEntities(requiredEntities, beanPropDep);
            } else if (availableEntities.size() == requiredEntities.size()) {
                // All created entities are already stored so use the stored ones for update
                handler.setDefaultEntities(availableEntities, beanPropDep);
            } else {
                // Mix, privilege stored entities, otherwise created entities

                // Before compose list of relevant value based on field annotated with unique attribute set to true.
                final var availableEntityValues = availableEntities.stream()
                        .collect(Collectors.toMap(
                                entity -> beanPropDep.stream()
                                        .map(rule -> entity.getPropertyValue(rule.getTo()))
                                        .toList(),
                                entity -> entity)
                        );

                // Get available entity (DB) otherwise required (pre-mapped)
                final var requiredAndAvailableEntities = requiredEntities.stream()
                        .map(entity -> getAvailableEntity(entity, beanPropDep, availableEntityValues).orElse(entity))
                        .toList();

                handler.setDefaultEntities(requiredAndAvailableEntities, beanPropDep);
            }
        }

        // Remove mapping associated to field annotated with unique attribute set to true (field associated to 'to'
        // clause of the mapping)
        // for each stream of the entity mapping, we keep the ones NOT contained in any of the dependency mappings
        final var mappingWithoutDependencies = processorDesc.getMappings()
                .stream()
                .filter(m -> beanPropDep.stream()
                        .filter(mwod -> mwod.getTo().equals(m.getTo()))
                        .toList().isEmpty())
                .toList();

        // Update entities set into the handler earlier
        var res = mapper.map(mappingWithoutDependencies, handler);

        // Check result (remove duplicate and entity where field must not be null)
        final var dependencies = processorDesc.getEntityMetadata()
                .getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            final var map = res.stream()
                    // Keep only entities that don't have field value set to null and must be unique
                    .filter(entity -> mustNotBeNullDependencies(dependencies, entity))
                    // Group entities which have same set of values (use to find duplicate)
                    .collect(Collectors.groupingBy(entity -> groupByDependencies(dependencies, entity)));

            map.forEach((key, value) -> {
                if (value.size() > 1) {
                    throw new ProcessorException("Cannot have multiple entities for dependencies %s: %s".formatted(key, value));
                }
            });
            return map.values().stream().flatMap(Collection::stream).toList();
        } else {
            return res;
        }
    }

    /**
     * Check that given entity does not have null value for given field
     *
     * @param dependencies field to check for null value
     * @param beanEntity entity to check
     * @return true if value for all fields are not null otherwise false.
     */
    private boolean mustNotBeNullDependencies(Collection<Field> dependencies, BeanAccessor beanEntity) {
        return dependencies.stream()
                // Keep only field which hase column annotation with unique attribute set to true
                .filter(field -> field.getAnnotation(Column.class).unique())
                .map(field -> {
                    // Compute path to access value
                    final var propName = "%s.%s".formatted(
                            beanEntity.getDelegate().getWrappedClass().getSimpleName(),
                            field.getName()
                    );
                    // Access value with the computed path
                    return beanEntity.getPropertyValue(new BeanProperty(propName)) != null;
                }).reduce(true, (l, n) -> l && n);
    }

    /**
     * Extract list of value for the given field on the given entity
     *
     * @param dependencies list of field to extract value
     * @param beanEntity entity on which to extract values
     * @return list of extracted value on the entity
     */
    private List<Object> groupByDependencies(Collection<Field> dependencies, BeanAccessor beanEntity) {
        return dependencies.stream()
                .map(field -> {
                    // Compute path to make value accessible by BeanAccessor
                    final var propName = "%s.%s".formatted(
                            beanEntity.getDelegate().getWrappedClass().getSimpleName(),
                            field.getName()
                    );
                    // Access value with the computed path
                    return beanEntity.getPropertyValue(new BeanProperty(propName));
                }).toList();
    }
}