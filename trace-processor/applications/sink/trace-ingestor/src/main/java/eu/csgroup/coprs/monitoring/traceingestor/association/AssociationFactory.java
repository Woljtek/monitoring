package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class AssociationFactory {
    private static final AssociationFactory INSTANCE = new AssociationFactory();

    private final Map<Entry, Constructor<DefaultAssociation>> cache = new HashMap<>();

    private AssociationFactory() {

    }


    public static AssociationFactory getInstance() {
        return INSTANCE;
    }

    public DefaultAssociation selectAssociation (Class<? extends DefaultEntity> containerClass, Class<? extends DefaultEntity> referenceClass, Deque<Field> associationFields) {
        final var entry = new Entry(containerClass, referenceClass);

        var entityAssociation = cache.get(entry);
        if (entityAssociation == null) {
            try {
                final var className = Class.forName("%s.%sTo%sAssociation"
                        .formatted(DefaultAssociation.class.getPackageName(), containerClass.getSimpleName(), referenceClass.getSimpleName()));

                entityAssociation = getConstructor(className);
            } catch (NoSuchMethodException | ClassNotFoundException e1) {
                try {
                    entityAssociation = getConstructor(DefaultAssociation.class);
                } catch (NoSuchMethodException e2) {
                    throw new AssociationException("Unable to retrieve constructor for default association class", e2);
                }
            }

            cache.put(entry, entityAssociation);
        }

        try {
            return entityAssociation.newInstance(associationFields == null ? new LinkedList<Field>() : associationFields);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new AssociationException(
                    "Unable to instantiate association class between %s and %s".formatted(
                            containerClass.getSimpleName(),
                            referenceClass.getSimpleName()),
                    e
            );
        }


    }

    @SuppressWarnings("unchecked")
    Constructor<DefaultAssociation> getConstructor(Class<?> defaultAssociationClass) throws NoSuchMethodException{
        return ((Class<DefaultAssociation>)defaultAssociationClass).getConstructor(Deque.class);
    }
    private record Entry(Class<? extends DefaultEntity> containerClass, Class<? extends DefaultEntity> referenceClass) {
    }
}
