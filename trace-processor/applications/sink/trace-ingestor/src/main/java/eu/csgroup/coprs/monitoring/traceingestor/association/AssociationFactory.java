<<<<<<< HEAD
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
||||||| b8aeece
=======
package eu.csgroup.coprs.monitoring.traceingestor.association;

import lombok.Data;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class AssociationFactory {
    private static final AssociationFactory INSTANCE = new AssociationFactory();

    private final Map<Entry, Constructor<DefaultAssociation>> cache = new HashMap<>();

    private AssociationFactory() {

    }


    public static AssociationFactory getInstance() {
        return INSTANCE;
    }

    public DefaultAssociation selectAssociation (Class containerClass, Class referenceClass, Deque<Field> associationFields) {
        final var entry = new Entry(containerClass, referenceClass);

        var entityAssociation = cache.get(entry);
        if (entityAssociation == null) {
            try {
                final var className = Class.forName("%s.%sTo%sAssociation"
                        .formatted(DefaultAssociation.class.getPackageName(), containerClass.getSimpleName(), referenceClass.getSimpleName()));
                entityAssociation = ((Class<DefaultAssociation>)className).getConstructor(Class.class, Deque.class);
            } catch (NoSuchMethodException | ClassNotFoundException e1) {
                try {
                    entityAssociation = DefaultAssociation.class.getConstructor(Class.class, Deque.class);
                } catch (NoSuchMethodException e2) {
                    // TODO
                    throw new RuntimeException(e2);
                }
            }

            cache.put(entry, entityAssociation);
        }

        try {
            return entityAssociation.newInstance(containerClass, associationFields);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }


    }

    @Data
    private static class Entry {
        private final Class containerClass;
        private final Class referenceClass;
    }
}
>>>>>>> dev
