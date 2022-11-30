<<<<<<< HEAD
package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityState;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public class DefaultAssociation implements EntityAssociation {

    private final Deque<Field> associationFields;

    /**
     * Create relation with each references by using entity container and creating copy for the others.
     *
     * @param entityContainer Entity in which to associate a reference and create a copy for the others.
     * @param references Reference list for which to create association with entity container.
     * @param entityFinder Instance that will process search in database.
     * @return All copy of entity container (including original)
     */
    public List<EntityProcessing> associate(EntityProcessing entityContainer, List<EntityProcessing> references, EntityFinder entityFinder) {
        final var associatedEntities = new LinkedList<EntityProcessing>();

        // Use original entity container for the first association
        boolean copy = false;

        final var entIt = references.iterator();
        EntityProcessing currentIt;

        while (entIt.hasNext()) {
            currentIt = entIt.next();

            associatedEntities.add(associate(entityContainer, currentIt, copy));

            // Then create a copy of the entity container.
            copy = true;
        }

        return associatedEntities;
    }

    protected  EntityProcessing associate(EntityProcessing entityContainer, EntityProcessing reference, boolean copy) {
        try {
            // Create copy or use the original.
            final var entityCopy = copy ? EntityHelper.copy(entityContainer.getEntity()) : entityContainer.getEntity();
            final var containerRef = EntityProcessing.fromEntity(entityCopy, EntityState.UNCHANGED);
            // If association field size is greater than 1 it means that we are associating a reference
            final var iter = associationFields.iterator();

            var path = "entity";
            while (iter.hasNext()) {
                path = PropertyUtil.getPath(path, iter.next().getName());
            }

            containerRef.setPropertyValue(new BeanProperty(path), reference.getEntity());
            return containerRef;
        } catch (Exception e) {
            throw new AssociationException(
                    "Cannot associate entity reference %s to entity container %s".formatted(
                            reference.getClass().getSimpleName(),
                            entityContainer.getClass().getSimpleName()
                    ), e
            );
        }
    }
}
||||||| b8aeece
=======
package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public class DefaultAssociation<C extends DefaultEntity, R extends DefaultEntity> {

    private final Class containerClass;

    private final Deque<Field> associationFields;

    private Method containerMethod;

    public List<DefaultEntity> associate(C entityContainer, List<R> references, EntityFinder entityFinder) {
        final var associatedEntities = new LinkedList<DefaultEntity>();

        boolean copy = false;

        final var entIt = references.iterator();
        DefaultEntity currentIt = null;

        while (entIt.hasNext()) {
            currentIt = entIt.next();

            associatedEntities.add(associate(entityContainer, currentIt, copy));

            copy = true;
        }

        return associatedEntities;
    }

    protected DefaultEntity associate(DefaultEntity entityContainer, DefaultEntity reference, boolean copy) {
        try {
            final var containerRef = copy ? EntityHelper.copy(entityContainer) : entityContainer;
            final var iter = associationFields.iterator();

            var currentField = iter.next();
            Object currentEntity = containerRef;
            while (iter.hasNext()) {
                currentEntity = getMethod(currentEntity.getClass(), "get", currentField).invoke(currentEntity);
                currentField = iter.next();
            }

            getMethod(currentEntity.getClass(), "set", currentField).invoke(currentEntity, reference);
            return containerRef;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    private Method getMethod(Class cls, String op, Field field) throws NoSuchMethodException {
            final var methodName = "%s%s".formatted(op, PropertyUtil.snake2PascalCasePropertyName(field.getName()));
            if ("set".equals(op)) {
                containerMethod = cls.getMethod(methodName, field.getType());
            } else {
                containerMethod = cls.getMethod(methodName);
            }

        return containerMethod;
    }

    /*public Ingestion getIngestionConfigFromContainer(Ingestion containerConfig) {
        return new Ingestion(containerConfig.getName(), new LinkedList<>(), new LinkedList<>(), new HashMap<>());
    }*/
}
>>>>>>> dev
