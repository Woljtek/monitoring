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
public class DefaultAssociation<C extends DefaultEntity, R extends DefaultEntity> implements EntityAssociation<C,R> {

    private final Deque<Field> associationFields;

    /**
     * Create relation with each references by using entity container and creating copy for the others.
     *
     * @param entityContainer Entity in which to associate a reference and create a copy for the others.
     * @param references Reference list for which to create association with entity container.
     * @param entityFinder Instance that will process search in database.
     * @return All copy of entity container (including original)
     */
    @Override
    public List<C> associate(C entityContainer, List<R> references, EntityFinder entityFinder) {
        final var associatedEntities = new LinkedList<C>();

        // Use original entity container for the first association
        boolean copy = false;

        final var entIt = references.iterator();
        R currentIt;

        while (entIt.hasNext()) {
            currentIt = entIt.next();

            associatedEntities.add(associate(entityContainer, currentIt, copy));

            // Then create a copy of the entity container.
            copy = true;
        }

        return associatedEntities;
    }

    protected C associate(C entityContainer, R reference, boolean copy) {
        try {
            // Create copy or use the original.
            final var containerRef = copy ? EntityHelper.copy(entityContainer) : entityContainer;
            // If association field size is greater than 1 it means that we are associating a reference
            final var iter = associationFields.iterator();

            // Use intermediate field as getter method
            var currentField = iter.next();
            Object currentEntity = containerRef;
            while (iter.hasNext()) {
                currentEntity = getMethod(currentEntity.getClass(), "get", currentField).invoke(currentEntity);
                currentField = iter.next();
            }

            // Use last field as setter method.
            getMethod(currentEntity.getClass(), "set", currentField).invoke(currentEntity, reference);
            return containerRef;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new AssociationException(
                    "Cannot associate entity reference %s to entity container %s".formatted(
                            reference.getClass().getSimpleName(),
                            entityContainer.getClass().getSimpleName()
                    ), e
            );
        }
    }

    private Method getMethod(Class<?> cls, String op, Field field) throws NoSuchMethodException {
            final var methodName = "%s%s".formatted(op, PropertyUtil.snake2PascalCasePropertyName(field.getName()));
        Method containerMethod;
        if ("set".equals(op)) {
                containerMethod = cls.getMethod(methodName, field.getType());
            } else {
                containerMethod = cls.getMethod(methodName);
            }

        return containerMethod;
    }
}
