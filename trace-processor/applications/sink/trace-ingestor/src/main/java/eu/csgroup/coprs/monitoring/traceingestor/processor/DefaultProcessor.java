package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.TraceMapper;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultProcessor<T extends DefaultEntity> extends AbstractProcessor<BeanAccessor, T> {
    public DefaultProcessor (
            String entityName,
            String configurationName,
            List<Mapping> mappings,
            List<BeanProperty> dependencies,
            EntityFinder entityFinder
    ) {
        super(entityName, configurationName, mappings, dependencies, entityFinder);
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
        final var requiredEntities = mapper.map(beanAccessor, mappingWithOnlyDependencies, configurationName, handler);
        final var availableEntities = (List<T>) entityFinder.findAll(getFindClauses(beanAccessor), handler.getEntityClass());

        // Feature: Handle case where parts of entities are not available (compared to required entities)
        if (availableEntities.isEmpty()) {
            handler.mergeWith(requiredEntities);
        } else if (availableEntities.size() == requiredEntities.size()) {
            handler.mergeWith(availableEntities);
        } else {
            throw new UnsupportedOperationException("Can't handle merge and create operation simultaneously");
        }



        final var mappingWithoutDependencies = mappings.stream().filter(m -> ! dependencies.contains(m.entityPath())).collect(Collectors.toList());
        return mapper.map(beanAccessor, mappingWithoutDependencies, configurationName, handler);
    }
}
