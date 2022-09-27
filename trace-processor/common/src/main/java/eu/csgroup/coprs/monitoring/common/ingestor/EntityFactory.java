package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.Entity;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityFactory {
    private static final EntityFactory INSTANCE = new EntityFactory();

    private final Map<Class<? extends DefaultEntity>, EntityMetadata> cache;

    private EntityFactory() {
        cache = parse();
        cache.values().forEach(this::postCreate);
    }

    public static EntityFactory getInstance() {
        return INSTANCE;
    }

    public EntityMetadata getMetadata(Class<?> entityClass) {
        return cache.get(entityClass);
    }

    private Map<Class<? extends DefaultEntity>, EntityMetadata> parse() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

       return scanner.findCandidateComponents(EntityIngestor.BASE_PACKAGE)
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .map(this::createMetadata)
                .collect(Collectors.toMap(EntityMetadata::getEntityClass, m -> m));
    }

    private EntityMetadata createMetadata (String entityClassName) {
        final var entityClass = EntityHelper.getEntityClass(entityClassName);
        final var dependencies = EntityHelper.getDependencies(entityClass);
        final var metadata = new EntityMetadata();

        metadata.setEntityClass(entityClass);
        metadata.setEntityName(entityClass.getSimpleName());
        metadata.setDependencies(dependencies);

        return metadata;
    }

    private void postCreate(EntityMetadata metadata) {
        final var entityClass = metadata.getEntityClass();
        final var relyOn = EntityHelper.relyOn(metadata.getEntityClass())
            .entrySet()
                .stream()
                .map(entry -> Map.entry(cache.get(entry.getKey()), entry.getValue())
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Set in which entity it is referenced (used to save entity before doing association)
        // Ensure that entity is unique
        relyOn.keySet()
                .stream()
                .map(em -> cache.get(em.getEntityClass()))
                .forEach(relyOnMetadata -> relyOnMetadata.addReferencedBy(entityClass));

        // Set entity which are parent of other entity (use child entity for association instead of parent)
        EntityHelper.getParentEntity(metadata.getEntityClass())
                .ifPresent(parent -> cache.get(parent).addChild(entityClass));

        metadata.setRelyOn(relyOn);
    }
}
