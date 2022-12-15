package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.Entity;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scan package {@link EntityIngestor#BASE_PACKAGE} to look for entity annotated with {@link Entity}. For each entity
 * found create an {@link EntityMetadata} instance that define:
 * <ul>
 *     <li>field dependencies (fields annotated with {@link javax.persistence.Column} with 'unique' attribute set to true)</li>
 *     <li>entities relation (fields which are an entity) for instance {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.MissingProducts#processing}</li>
 *     <li>entities that reference this entity</li>
 *     <li>child entities (for instance {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib} is a child of
 *     {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput})</li>
 * </ul>
 *
 * Construct metadata information on entity which implements {@link DefaultEntity} interface which are located in this
 * package {@link EntityIngestor#BASE_PACKAGE}
 */
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
        // Create package scanner
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        // Define how to find an entity
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        // Execute scanner and for each result construct metadata
        return scanner.findCandidateComponents(EntityIngestor.BASE_PACKAGE)
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .map(this::createMetadata)
                .collect(Collectors.toMap(EntityMetadata::getEntityClass, m -> m));
    }

    /**
     * Create metadata for the given class name with only dependencies set (need to create metadata of other entities
     * to fill other metadata)
     *
     * @param entityClassName entity class name
     * @return entity metadata
     */
    private EntityMetadata createMetadata (String entityClassName) {
        final var entityClass = EntityHelper.getEntityClass(entityClassName);
        final var dependencies = EntityHelper.getDependencies(entityClass);
        final var metadata = new EntityMetadata();

        metadata.setEntityClass(entityClass);
        metadata.setEntityName(entityClass.getSimpleName());
        metadata.setDependencies(dependencies);

        return metadata;
    }

    /**
     * Complete metadata with entities relation, reference and child
     *
     * @param metadata entity metadata to update
     */
    private void postCreate(EntityMetadata metadata) {
        final var entityClass = metadata.getEntityClass();
        final var relyOn = EntityHelper.relyOn(entityClass)
                .entrySet()
                .stream()
                .map(entry -> Map.entry(cache.get(entry.getKey()), entry.getValue())
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Set in which entity it is referenced (used to save entity before doing association)
        // Ensure that entity is unique
        relyOn.keySet()
                .forEach(relyOnMetadata -> relyOnMetadata.addReferencedBy(entityClass));

        // Set entity which are parent of other entity (use child entity for association instead of parent)
        EntityHelper.getParentEntity(entityClass)
                .ifPresent(parent -> cache.get(parent).addChild(entityClass));

        metadata.setRelyOn(relyOn);
    }
}