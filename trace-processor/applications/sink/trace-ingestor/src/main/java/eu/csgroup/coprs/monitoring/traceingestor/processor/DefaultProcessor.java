package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
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
import java.util.stream.Collectors;

@Slf4j
public class DefaultProcessor<T extends DefaultEntity> extends AbstractProcessor<BeanAccessor, T> {
    public DefaultProcessor (
            ProcessorDescription processorDesc,
            EntityFinder entityFinder
    ) {
        super(processorDesc, entityFinder);
    }

    private Specification<T> getFindClauses(Map<Mapping, Object> dependenciesValue ) {
        // Feature: Handle arrays equality case
        return dependenciesValue.entrySet().stream()
                .map(entry -> EntitySpecification.<T>getEntityBy(
                        entry.getKey().getTo().getRawBeanPropertyPath(),
                        entry.getValue())
                ).reduce(Specification.where(null), Specification::and);
    }

    private List<Mapping> getBeanPropertyDependencies () {
         return processorDesc.getEntityMetadata()
                 .getDependencies()
                 .stream()
                 .map(field -> processorDesc.getIngestionConfig()
                         .getMappings()
                         .stream()
                         .filter(mapping -> mapping.getTo().getBeanPropertyPath().equals(field.getName()))
                         .findFirst()
                         .orElse(null)
                 ).filter(Objects::nonNull)
                 .toList();
    }

    private Collection<TreePropertyLeaf> extractLeafs (TreePropertyNode tree) {
        return new ArrayList<>(tree.getLeafs().values());
    }

    private Object reducePropertyValues (List<TreePropertyLeaf> leafs) {
        if (leafs.size() == 1) {
            final var leaf = leafs.get(0);
            return TraceMapper.mapPropertyValue(leaf.getRule(), leaf.getRawValue());
        } else {
            return leafs.stream()
                    .map(leaf -> TraceMapper.mapPropertyValue(leaf.getRule(), leaf.getRawValue()))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private Optional<T> getAvailableEntity(T requiredEntity, List<Mapping> beanPropDep, Map<List<Object>, T> availableEntityValues, DefaultHandler<T> handler) {
        final var requiredEntityBean = handler.getWrapper(requiredEntity);

        final var requiredEntityValues = beanPropDep.stream()
                .map(rule -> requiredEntityBean.getPropertyValue(rule.getTo()))
                .toList();

        return Optional.ofNullable(availableEntityValues.get(requiredEntityValues));
    }

    @Override
    public List<T> apply(BeanAccessor beanAccessor) {
        final var handler = new DefaultHandler<T>(processorDesc.getEntityMetadata().getEntityClass());
        final var mapper = new TraceMapper<T>(beanAccessor, processorDesc.getIngestionConfig().getName());
        final var treePropertyValue = new Parser(processorDesc.getIngestionConfig().getMappings()).parse(beanAccessor);

        final var availableEntities = new ArrayList<T>();


        // Retrieve mapping that refer to an entity field marked as unique.
        final var beanPropDep = getBeanPropertyDependencies();

        if (beanPropDep.size() != 0) {
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
                availableEntities.addAll(entityFinder.findAll(
                        getFindClauses(dependencyValue),
                        handler.getEntityClass())
                );
            }

            final var requiredEntities = mapper.map(treePropertyValue, handler)
                    .stream().map(bean -> (T)bean.getDelegate().getWrappedInstance())
                    .toList();

            log.debug("Number of available entities %s".formatted(availableEntities));
            log.debug("Number of required entities %s".formatted(requiredEntities));

            if (availableEntities.isEmpty()) {
                handler.mergeWith(requiredEntities);
            } else if (availableEntities.size() == requiredEntities.size()) {
                handler.mergeWith(availableEntities);
            } else {
                final var availableEntityValues = availableEntities.stream()
                        .collect(Collectors.toMap(
                                entity ->  {
                                    final var entityBean = handler.getWrapper(entity);
                                    return beanPropDep.stream()
                                            .map(rule -> entityBean.getPropertyValue(rule.getTo()))
                                            .toList();
                                },
                                entity -> entity)
                        );

                // Get available entity (DB) otherwise required (pre-mapped)
                final var requiredAndAvailableEntities = requiredEntities.stream()
                        .map(entity -> getAvailableEntity(entity, beanPropDep, availableEntityValues, handler).orElse(entity))
                        .toList();

                handler.mergeWith(requiredAndAvailableEntities);
            }
        }

        final var mappingWithoutDependencies = processorDesc.getIngestionConfig()
                .getMappings()
                .stream()
                .filter(m -> beanPropDep.stream()
                        .filter(mwod -> mwod.getTo().equals(m.getTo()))
                        .toList().isEmpty())
                        .toList();
        var res = mapper.map(mappingWithoutDependencies, handler);

        // Check result (remove duplicate and entity where field must not be null)
        final var dependencies = processorDesc.getEntityMetadata()
                .getDependencies();
        if (dependencies != null && ! dependencies.isEmpty()) {
            final var map = res.stream()
                    .filter(entity -> mustNotBeNullDependencies(dependencies, entity))
                    .collect(Collectors.groupingBy(entity -> groupByDependencies(dependencies, entity)))
                    .entrySet()
                    .stream()
                    // Check if grouped entity are equal (try to reduce)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue()
                                    .stream()
                                    .map(bean -> (T)bean.getDelegate().getWrappedInstance())
                                    .collect(Collectors.toSet()))
                    );

            map.forEach((key, value) -> {
                if (value.size() > 1) {
                    throw new RuntimeException("Cannot have multiple entities for dependencies %s: %s".formatted(key, value));
                }
            });
            return map.values().stream().flatMap(Collection::stream).toList();
        } else {
            return res.stream().map(bean -> (T)bean.getDelegate().getWrappedInstance()).toList();
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
                }).reduce(true, (l,n) -> l && n);
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
