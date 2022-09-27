package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityHelper {

    private EntityHelper () {

    }

    public static Map<Class<DefaultEntity>, Deque<Field>> relyOn (Class<DefaultEntity> entityClass) {
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

    public static Collection<Field> getDependencies (Class<DefaultEntity> entityClass) {
        return parseClassHierarchy(entityClass).stream()
                .flatMap(uClass -> Arrays.stream(uClass.getDeclaredFields()))
                .filter(field -> Arrays.stream(field.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType().equals(Column.class)
                                && ((Column)annotation).unique())
                ).toList();
    }

    public static Optional<Class<DefaultEntity>> getParentEntity (Class<DefaultEntity> childEntity) {
        return parseClassHierarchy(childEntity).stream()
                .filter(parent -> ! parent.equals(childEntity))
                .findFirst();
    }

    public static List<Class<DefaultEntity>> parseClassHierarchy(Class<DefaultEntity> parsableClass) {
        final var classL = new ArrayList<Class<DefaultEntity>>();
        Class<DefaultEntity> current = parsableClass;

        do {
            classL.add(current);

            current = castClassToEntityClass(current.getSuperclass());
        } while(current.getSuperclass() != null && current.getAnnotation(Entity.class) != null);

        return classL;
    }

    public static Stream<Class<DefaultEntity>> getDeepRelyOn (EntityMetadata metadata) {
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

    public static Stream<Class<DefaultEntity>> getDeepReferencedBy (EntityMetadata metadata) {
        return metadata.getReferencedBy()
                .stream()
                .map(entityClass -> EntityFactory.getInstance().getMetadata(entityClass))
                .flatMap(referencedByMetadata -> Stream.concat(
                        Stream.of(referencedByMetadata.getEntityClass()),
                        getDeepReferencedBy(referencedByMetadata)
                ));
    }

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

    public static <E extends DefaultEntity> E copy (E entity) {
        return copy(entity, false);
    }

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
