package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to collect entity metadata such as:
 * <ul>
 *   <li>field dependencies (fields annotated with {@link javax.persistence.Column} with 'unique' attribute set to true)</li>
 *   <li>entities relation (fields which are an entity) for instance {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.MissingProducts#processing}</li>
 *   <li>entities that reference this entity</li>
 *   <li>child entities (for instance {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib} is a child of
 *   {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput})</li>
 * </ul>
 */
public class EntityHelper {

    private EntityHelper () {

    }

    /**
     * Find on which entity the given one rely on by using the following annotations:
     * <ul>
     *     <li>{@link OneToMany}</li>
     *     <li>{@link OneToOne}</li>
     *     <li>{@link ManyToMany}</li>
     *     <li>{@link ManyToOne}</li>
     *     <li>{@link EmbeddedId}</li>
     * </ul>
     * For {@link EmbeddedId} annotation, this is an entry point telling to look deeper for relation on this field
     * (for example {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.InputListExternalId} in
     * {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.InputListInternal})
     *
     * @param entityClass relation to find for the class
     * @return entities which are needed for the given entity class
     */
    public static Map<Class<DefaultEntity>, Deque<Field>> relyOn (Class<? extends DefaultEntity> entityClass) {
        return parseClassHierarchy(entityClass).stream()
                .flatMap(uClass -> Arrays.stream(uClass.getDeclaredFields()))
                .filter(field -> Arrays.stream(field.getAnnotations())
                        .map(Annotation::annotationType)
                        .anyMatch(annotation -> annotation.equals(OneToMany.class)
                                || annotation.equals(OneToOne.class)
                                || annotation.equals(ManyToMany.class)
                                || annotation.equals(ManyToOne.class)
                                || annotation.equals(EmbeddedId.class))
                ).map(EntityHelper::relyOn)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Find relation for the class defined by the given field. Lookup is limited to field annotated with {@link EmbeddedId}
     * which is a class that define a complex id composed of multiple entity<br>
     * <br>
     * For field which hasn't this annotation simply return the class defined by the given field as a result
     *
     * @param field Field that enclose class to look for relation
     * @return relation with the class contained in field otherwise the class itself as result (in case of field is not
     * annotated with {@link EmbeddedId})
     */
    private static Map<Class<DefaultEntity>, Deque<Field>> relyOn(Field field) {
        if (field.getAnnotation(EmbeddedId.class) != null) {
            final var relyOn = relyOn((castClassToEntityClass(field.getType())));
            relyOn.forEach((key, value) -> value.addFirst(field));

            return relyOn;
        } else {
            final var fields = new LinkedList<Field>();
            fields.add(field);
            return Map.of((castClassToEntityClass(field.getType())), fields);
        }
    }

    /**
     * For the given class, find field which are annotated with {@link Column} where 'unique' attribute is set to true
     * (principally used to determine query to look for a specific entity in storage)
     *
     * @param entityClass class to look for dependencies
     * @return list with field annotated with {@link Column} and 'unique' attribute set to true otherwise empty list if
     * none found
     */
    public static Collection<Field> getDependencies (Class<DefaultEntity> entityClass) {
        return parseClassHierarchy(entityClass).stream()
                .flatMap(uClass -> Arrays.stream(uClass.getDeclaredFields()))
                .filter(field -> Arrays.stream(field.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType().equals(Column.class)
                                && ((Column)annotation).unique())
                ).toList();
    }

    /**
     * Get the parent class only if it's annotated with {@link Entity}
     *
     * @param childEntity child entity
     * @return parent class if annotated it's with {@link Entity} oherwise empty result.
     */
    public static Optional<Class<? extends DefaultEntity>> getParentEntity (Class<? extends DefaultEntity> childEntity) {
        return parseClassHierarchy(childEntity).stream()
                .filter(parent -> ! parent.equals(childEntity))
                .findFirst();
    }

    /**
     * Get parent class of the given one recursively until the class is not annotated with {@link Entity}
     *
     * @param parsableClass class to look for parent
     * @return list of class which are annotated with {@link Entity} (including the given class).
     */
    public static List<Class<? extends DefaultEntity>> parseClassHierarchy(Class<? extends DefaultEntity> parsableClass) {
        final var classL = new ArrayList<Class<? extends DefaultEntity>>();
        Class<? extends DefaultEntity> current = parsableClass;

        do {
            classL.add(current);

            current = castClassToEntityClass(current.getSuperclass());
        } while(current.getSuperclass() != null && current.getAnnotation(Entity.class) != null);

        return classL;
    }

    /**
     * Find relation of the entity defined in metadata and for each relation found, find the others recursively.<br>
     * <br>
     * For example if we are looking for relation of {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.InputListExternal}
     * this will give the following result:
     * <ul>
     *     <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.Processing}</li>
     *     <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput}
     *          <ul>
     *              <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib}</li>
     *              <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk}</li>
     *              <li>{@link  eu.csgroup.coprs.monitoring.common.datamodel.entities.AuxData}</li>
     *          </ul>
     *     </li>
     * </ul>
     *
     * @param metadata Metadata containing entity to look for relation with other entity
     * @return Stream of entity that are in relation with the given directly and indirectly.
     */
    public static Stream<Class<? extends DefaultEntity>> getDeepRelyOn (EntityMetadata metadata) {
        return metadata.getRelyOn()
                .keySet()
                .stream()
                .flatMap(relyOnMetadata -> Stream.concat(
                        Stream.of(relyOnMetadata.getEntityClass()),
                        Stream.concat(
                                relyOnMetadata.getChild()
                                        .stream()
                                        .map(EntityFactory.getInstance()::getMetadata)
                                        .flatMap(relyOnMetadataChild -> Stream.concat(
                                                Stream.of(relyOnMetadataChild.getEntityClass()),
                                                getDeepRelyOn(relyOnMetadataChild))
                                        ),
                                getDeepRelyOn(relyOnMetadata)
                        ))
                );
    }

    /**
     * Find entity that references the entity set in given metadata and for each reference found, find their reference
     * recursively.<br>
     * <br>
     * For example, if we are looking for entity which reference {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.Processing}
     * we will have the following:
     * <ul>
     *     <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.MissingProducts}</li>
     *     <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.OutputList}</li>
     *     <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.InputListInternal} (by the intermediate of
     *     {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.InputListInternalId})</li>
     *     <li>{@link eu.csgroup.coprs.monitoring.common.datamodel.entities.InputListExternal} (by the intermediate of
     *     {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.InputListExternalId}</li>
     * </ul>
     *
     * @param metadata Metadata containing entity to look for in other entity
     * @return list of entity that reference the given one
     */
    public static Stream<Class<? extends DefaultEntity>> getDeepReferencedBy (EntityMetadata metadata) {
        return metadata.getReferencedBy()
                .stream()
                .map(entityClass -> EntityFactory.getInstance().getMetadata(entityClass))
                .flatMap(referencedByMetadata -> Stream.concat(
                        Stream.of(referencedByMetadata.getEntityClass()),
                        getDeepReferencedBy(referencedByMetadata)
                ));
    }

    /**
     * From the given entity name get the associated class by looking in {@link EntityIngestor#BASE_PACKAGE} for a class
     * that share the same given name.
     *
     * @param entityName class name to look for
     * @return Class associated to the given name otherwise an exception
     * @throws EntityException if there is no class found sharing the same given name
     */
    public static  Class<DefaultEntity> getEntityClass(String entityName) {
        try {
            String completeEntityName;
            if (entityName.startsWith(EntityIngestor.BASE_PACKAGE)) {
                completeEntityName = entityName;
            } else {
                completeEntityName = "%s.%s".formatted(EntityIngestor.BASE_PACKAGE, entityName);
            }
            return castClassToEntityClass(Class.forName(completeEntityName));
        } catch (ClassNotFoundException e) {
            throw new EntityException("Entity with name %s does not exists".formatted(entityName), e);
        }
    }

    /**
     * Create an exact copy of the given entity
     *
     * @param entity Entity to duplicate
     * @return A copy of the given entity
     */
    public static <E extends DefaultEntity> E copy (E entity) {
        return copy(entity, false);
    }

    /**
     * Create a copy of the given entity by resetting id
     *
     * @param entity Entity to duplicate
     * @param resetId Reset id
     * @return A copy of the given entity
     */
    @SuppressWarnings("unchecked")
    public static <E extends DefaultEntity> E copy (E entity, boolean resetId) {
        final var copy = entity.copy();

        if (resetId) {
            copy.resetId();
        }

        return (E) copy;
    }

    @SuppressWarnings("unchecked")
    private static Class<DefaultEntity> castClassToEntityClass (Class<?> noEntityClass) {
        return (Class<DefaultEntity>)noEntityClass;
    }
}