package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityHelper {

    public static Map<Class, Deque<Field>> relyOn (Class entityClass) {
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

    private static Map<Class, Deque<Field>> relyOn(Field field) {
        if (field.getAnnotation(EmbeddedId.class) != null) {
            final var relyOn = relyOn(field.getType());
            relyOn.entrySet().stream().forEach(entry -> entry.getValue().addFirst(field));

            return relyOn;
        } else {
            final var fields = new LinkedList<Field>();
            fields.add(field);
            return Map.of(field.getType(), fields);
        }
    }

    public static Collection<Field> getDependencies (Class entityClass) {
        return parseClassHierarchy(entityClass).stream()
                .flatMap(uClass -> Arrays.stream(uClass.getDeclaredFields()))
                .filter(field -> Arrays.stream(field.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType().equals(Column.class)
                                && ((Column)annotation).unique())
                ).toList();
    }

    public static List<Class<DefaultEntity>> findRelationFor (Class entityClass) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        return scanner.findCandidateComponents(EntityIngestor.BASE_PACKAGE)
                .stream()
                .map(beanDef -> beanDef.getBeanClassName())
                .map(EntityHelper::getEntityClass)
                .filter(foundClass -> relyOn(foundClass).containsKey(entityClass))
                .toList();

    }

    public static Optional<Class> getParentEntity (Class childEntity) {
        return parseClassHierarchy(childEntity).stream()
                .filter(parent -> ! parent.equals(childEntity))
                .findFirst();
    }

    public static List<Class> parseClassHierarchy(Class parsableClass) {
        final var classL = new Vector<Class>();
        Class current = parsableClass;

        do {
            classL.add(current);

            current = current.getSuperclass();
        } while(current.getSuperclass() != null && current.getAnnotation(Entity.class) != null);

        return classL;
    }

    public static Stream<Class> getDeepRelyOn (EntityMetadata metadata) {
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

    public static Stream<Class> getDeepReferencedBy (EntityMetadata metadata) {
        return metadata.getReferencedBy()
                .stream()
                .map(entityClass -> EntityFactory.getInstance().getMetadata(entityClass))
                .flatMap(referencedByMetadata -> Stream.concat(
                        Stream.of(referencedByMetadata.getEntityClass()),
                        getDeepReferencedBy(referencedByMetadata)
                ));
    }

    public static <E extends DefaultEntity> Class<E> getEntityClass(String entityName) {
        try {
            String completeEntityName;
            if (entityName.startsWith(EntityIngestor.BASE_PACKAGE)) {
                completeEntityName = entityName;
            } else {
                completeEntityName = "%s.%s".formatted(EntityIngestor.BASE_PACKAGE, entityName);
            }
            return (Class<E>) Class.forName(completeEntityName);
        } catch (ClassNotFoundException e) {
            // TODO
            throw new RuntimeException("Entity with name %s does not exists".formatted(entityName), e);
        }
    }

    public static <E extends DefaultEntity> E copy (E entity) {
        final var copy = (E)(entity.copy());
        copy.setId(null);

        return copy;
    }
}
