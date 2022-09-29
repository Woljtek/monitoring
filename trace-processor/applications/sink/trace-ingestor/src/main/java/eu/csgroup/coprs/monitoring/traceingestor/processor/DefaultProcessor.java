package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.TraceMapper;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
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

    private Specification<T> getFindClauses(BeanAccessor bean) {
        // Feature: Handle arrays equality case
        return getDependenciesValue(bean).entrySet().stream()
                .map(entry -> EntitySpecification.<T>getEntityBy(
                        entry.getKey().getRawBeanPropertyPath(),
                        entry.getValue())
                ).reduce(Specification.where(null), Specification::and);
    }

    private Map<BeanProperty, Object> getDependenciesValue(BeanAccessor bean) {
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
                .collect(Collectors.toMap(Mapping::getTo, m -> bean.getPropertyValue(m.getFrom())));
    }

    @Override
    public List<T> apply(BeanAccessor beanAccessor) {
        final var handler = new DefaultHandler<T>(processorDesc.getEntityMetadata().getEntityClass());
        final var mapper = new TraceMapper<T>();

        final var mappingWithOnlyDependencies = processorDesc.getEntityMetadata()
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
        final var requiredEntities = mapper.map(beanAccessor, mappingWithOnlyDependencies, processorDesc.getIngestionConfig().getName(), handler)
                .stream().map(bean -> (T)bean.getDelegate().getWrappedInstance())
                .toList();
        var availableEntities = List.<T>of();
        if (getDependenciesValue(beanAccessor).size() > 0) {
            availableEntities = entityFinder.findAll(getFindClauses(beanAccessor), handler.getEntityClass());
        }
        log.debug("Number of available entities %s".formatted(availableEntities));
        log.debug("Number of required entities %s".formatted(requiredEntities));

        // Feature: Handle case where parts of entities are not available (compared to required entities)
        if (availableEntities.isEmpty()) {
            handler.mergeWith(requiredEntities);
        } else if (availableEntities.size() == requiredEntities.size()) {
            handler.mergeWith(availableEntities);
        } else {
            throw new UnsupportedOperationException("Can't handle merge and create operation simultaneously");
        }

        final var mappingWithoutDependencies = processorDesc.getIngestionConfig()
                .getMappings()
                .stream()
                .filter(m -> mappingWithOnlyDependencies.stream()
                        .filter(mwod -> mwod.getTo().equals(m.getTo()))
                        .toList().isEmpty())
                        .toList();
        var res = mapper.map(beanAccessor, mappingWithoutDependencies, processorDesc.getIngestionConfig().getName(), handler);

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
