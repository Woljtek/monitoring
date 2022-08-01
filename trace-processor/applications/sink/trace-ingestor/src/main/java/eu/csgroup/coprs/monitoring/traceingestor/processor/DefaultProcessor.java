package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.TraceMapper;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class DefaultProcessor<T extends ExternalInput> extends AbstractProcessor<BeanAccessor, T> {
    public DefaultProcessor (
            String entityName,
            List<Mapping> mappings,
            List<BeanProperty> dependencies,
            BiFunction<Specification<? extends ExternalInput>, Class<? extends ExternalInput>, List<? extends ExternalInput>> entityFinder
    ) {
        super(entityName, mappings, dependencies, entityFinder);
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
        return dependencies.stream()
                .map(c -> mappings.stream()
                        .filter(m -> m.entityPath().equals(c))
                        .findFirst()
                        .get())
                .collect(Collectors.toMap(m -> m.entityPath(), m -> bean.getPropertyValue(m.tracePath())));
    }

    @Override
    public List<T> apply(BeanAccessor beanAccessor) {
        final var handler = new DefaultHandler<T>(entityName);
        final var mapper = new TraceMapper<T>();

        final var mappingWithOnlyDependencies = mappings.stream().filter(m -> dependencies.contains(m.entityPath())).collect(Collectors.toList());
        final var requiredEntities = mapper.map(beanAccessor, mappingWithOnlyDependencies, handler);
        final var availableEntities = (List<T>) entityFinder.apply(getFindClauses(beanAccessor), handler.getEntityClass());

        // Feature: Handle case where parts of entities are not available (compared to required entities)
        if (availableEntities.isEmpty()) {
            handler.mergeWith(requiredEntities);
        } else {
            handler.mergeWith(availableEntities);
        }


        final var mappingWithoutDependencies = mappings.stream().filter(m -> ! dependencies.contains(m.entityPath())).collect(Collectors.toList());
        return mapper.map(beanAccessor, mappingWithoutDependencies, handler);
    }
}
