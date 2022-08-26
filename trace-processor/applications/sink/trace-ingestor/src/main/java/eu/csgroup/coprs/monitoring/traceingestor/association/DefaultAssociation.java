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
