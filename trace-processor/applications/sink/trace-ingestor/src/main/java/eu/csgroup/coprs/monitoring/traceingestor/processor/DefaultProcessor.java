package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
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

@Slf4j
public record DefaultProcessor(
        String name,
        ProcessorDescription processorDesc,
        EntityFinder entityFinder) implements Function<BeanAccessor, List<EntityProcessing>> {
    private <T extends DefaultEntity> Specification<T> getFindClauses(Map<Mapping, Object> dependenciesValue) {
        // Feature: Handle arrays equality case
        return dependenciesValue.entrySet().stream()
                .map(entry -> EntitySpecification.<T>getEntityBy(
                        entry.getKey().getTo().getRawBeanPropertyPath(),
                        entry.getValue())
                ).reduce(Specification.where(null), Specification::and);
    }

    private List<Mapping> getBeanPropertyDependencies() {
        return processorDesc.getEntityMetadata()
                .getDependencies()
                .stream()
                .map(field -> processorDesc.getMappings()
                        .stream()
                        .filter(mapping -> mapping.getTo().getBeanPropertyPath().equals(field.getName()))
                        .findFirst()
                        .orElse(null)
                ).filter(Objects::nonNull)
                .toList();
    }

    private Collection<TreePropertyLeaf> extractLeafs(TreePropertyNode tree) {
        return new ArrayList<>(tree.getLeafs());
    }

    private Object reducePropertyValues(List<TreePropertyLeaf> leafs) {
        if (leafs.size() == 1) {
            final var leaf = leafs.get(0);
            return TraceMapper.mapPropertyValue(leaf.getRule(), leaf.getRawValues());
        } else {
            return leafs.stream()
                    .map(leaf -> TraceMapper.mapPropertyValue(leaf.getRule(), leaf.getRawValues()))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private Optional<EntityProcessing> getAvailableEntity(EntityProcessing requiredEntity, List<Mapping> beanPropDep, Map<List<Object>, EntityProcessing> availableEntityValues, DefaultHandler handler) {
        final var requiredEntityValues = beanPropDep.stream()
                .map(rule -> requiredEntity.getPropertyValue(rule.getTo()))
                .toList();

        return Optional.ofNullable(availableEntityValues.get(requiredEntityValues));
    }

    @Override
    public List<EntityProcessing> apply(BeanAccessor beanAccessor) {
        final var handler = new DefaultHandler(processorDesc.getEntityMetadata().getEntityClass());
        final var mapper = new TraceMapper(beanAccessor, this.name);
        final var treePropertyValue = new Parser(processorDesc.getMappings()).parse(beanAccessor);

        final var availableEntities = new ArrayList<EntityProcessing>();


        // Retrieve mapping that refer to an entity field marked as unique.
        final var beanPropDep = getBeanPropertyDependencies();

        if (!beanPropDep.isEmpty()) {
            // Can be single value or array
            final var dependencyValue = extractLeafs(treePropertyValue).stream()
                    .filter(leaf -> beanPropDep.contains(leaf.getRule()))
                    .collect(Collectors.groupingBy(TreePropertyLeaf::getRule))
                    .entrySet()
                    .stream()
                    .collect(Collectors
                            .toMap(Map.Entry::getKey, entry -> reducePropertyValues(entry.getValue()))
                    );

            if (dependencyValue.size() > 0) {
                availableEntities.addAll(
                        entityFinder.findAll(getFindClauses(dependencyValue), handler.getEntityClass()).stream()
                                .map(entity -> EntityProcessing.fromEntity(entity, EntityState.UNCHANGED))
                                .toList()

                );
            }

            final var requiredEntities = mapper.map(beanPropDep, handler);

            log.debug("Number of available entities %s".formatted(availableEntities));
            log.debug("Number of required entities %s".formatted(requiredEntities));

            if (availableEntities.isEmpty()) {
                handler.setDefaultEntities(requiredEntities, beanPropDep);
            } else if (availableEntities.size() == requiredEntities.size()) {
                handler.setDefaultEntities(availableEntities, beanPropDep);
            } else {
                final var availableEntityValues = availableEntities.stream()
                        .collect(Collectors.toMap(
                                entity -> beanPropDep.stream()
                                        .map(rule -> entity.getPropertyValue(rule.getTo()))
                                        .toList(),
                                entity -> entity)
                        );

                // Get available entity (DB) otherwise required (pre-mapped)
                final var requiredAndAvailableEntities = requiredEntities.stream()
                        .map(entity -> getAvailableEntity(entity, beanPropDep, availableEntityValues, handler).orElse(entity))
                        .toList();

                handler.setDefaultEntities(requiredAndAvailableEntities, beanPropDep);
            }
        }

        final var mappingWithoutDependencies = processorDesc.getMappings()
                .stream()
                .filter(m -> beanPropDep.stream()
                        .filter(mwod -> mwod.getTo().equals(m.getTo()))
                        .toList().isEmpty())
                .toList();
        var res = mapper.map(mappingWithoutDependencies, handler);

        // Check result (remove duplicate and entity where field must not be null)
        final var dependencies = processorDesc.getEntityMetadata()
                .getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            final var map = res.stream()
                    .filter(entity -> mustNotBeNullDependencies(dependencies, entity))
                    .collect(Collectors.groupingBy(entity -> groupByDependencies(dependencies, entity)))
                    .entrySet()
                    .stream()
                    // Check if grouped entity are equal (try to reduce)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue)
                    );

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

    private boolean mustNotBeNullDependencies(Collection<Field> dependencies, BeanAccessor beanEntity) {
        return dependencies.stream()
                .filter(field -> field.getAnnotation(Column.class).unique())
                .map(field -> {
                    final var propName = "%s.%s".formatted(
                            beanEntity.getDelegate().getWrappedClass().getSimpleName(),
                            field.getName()
                    );

                    return beanEntity.getPropertyValue(new BeanProperty(propName)) != null;
                }).reduce(true, (l, n) -> l && n);
    }

    private List<Object> groupByDependencies(Collection<Field> dependencies, BeanAccessor beanEntity) {
        return dependencies.stream()
                .map(field -> {
                    final var propName = "%s.%s".formatted(
                            beanEntity.getDelegate().getWrappedClass().getSimpleName(),
                            field.getName()
                    );

                    return beanEntity.getPropertyValue(new BeanProperty(propName));
                }).toList();
    }
}