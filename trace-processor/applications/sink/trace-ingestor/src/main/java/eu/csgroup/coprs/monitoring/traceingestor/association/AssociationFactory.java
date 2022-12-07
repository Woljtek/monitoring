package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Factory class to retrieve instance that must be used to do association between two entities
 * (i.e {@link eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk#setDsib(Dsib)}).<br>
 * <br>
 * For specialized association, class must be named with following pattern:
 * "{@literal <}container_class_name{@literal >}To{@literal <}reference_class_name{@literal >}Association"<br>
 * <br>
 * Container class is the entity class in which reference class must be associated in.
 * Parameters for specialized class must be:
 * <ul>
 *     <li>{@link Deque} of {@link Field}</li>
 * </ul>
 */
public class AssociationFactory {
    private static final String PATTERN_CLASS_NAME = "%s.%sTo%sAssociation";

    /**
     * Singleton instance
     */
    private static final AssociationFactory INSTANCE = new AssociationFactory();

    /**
     * Association between two entities is static so cache result to do same search n times
     */
    private final Map<Entry, Constructor<DefaultAssociation>> cache = new HashMap<>();

    /**
     * Singleton constructor
     */
    private AssociationFactory() {

    }


    public static AssociationFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Select instance to use to do association between two entities
     *
     * @param containerClass Entity class in which to set association to reference class
     * @param referenceClass Entity class that must be referenced in container class
     * @param associationFields Chained list of fields to use to do association of reference class in container class
     * @return Specialized instance or {@link DefaultAssociation} instance
     */
    public DefaultAssociation selectAssociation (Class<? extends DefaultEntity> containerClass, Class<? extends DefaultEntity> referenceClass, Deque<Field> associationFields) {
        final var entry = new Entry(containerClass, referenceClass);

        // Check if selection was already done
        var entityAssociation = cache.get(entry);

        // Not already done so select instance.
        if (entityAssociation == null) {
            // Try to find if there is a specialized instance
            try {
                final var className = Class.forName(PATTERN_CLASS_NAME
                        .formatted(DefaultAssociation.class.getPackageName(), containerClass.getSimpleName(), referenceClass.getSimpleName()));

                entityAssociation = getConstructor(className);
            } catch (NoSuchMethodException | ClassNotFoundException e1) {
                // If not use default one.
                try {
                    entityAssociation = getConstructor(DefaultAssociation.class);
                } catch (NoSuchMethodException e2) {
                    throw new AssociationException("Unable to retrieve constructor for default association class", e2);
                }
            }

            // Store result for later use.
            cache.put(entry, entityAssociation);
        }

        // Create an instance for the request
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

    /**
     * Retrieve constructor for the given class
     *
     * @param defaultAssociationClass association class to get constructor
     * @return association class constructor
     * @throws NoSuchMethodException if constructor class does not exists
     */
    @SuppressWarnings("unchecked")
    Constructor<DefaultAssociation> getConstructor(Class<?> defaultAssociationClass) throws NoSuchMethodException {
        return ((Class<DefaultAssociation>)defaultAssociationClass).getConstructor(Deque.class);
    }

    /**
     * Simple record to store for which entity class association constructor class is.
     *
     * @param containerClass
     * @param referenceClass
     */
    private record Entry(Class<? extends DefaultEntity> containerClass, Class<? extends DefaultEntity> referenceClass) {
    }
}
