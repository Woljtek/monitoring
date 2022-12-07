package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Default entity handler which provide an entity when requested.<br>
 * <br>
 * By default, entity provided is empty (no field set) or it's possible to register entities for later use.
 * Registered entities can be associated to a list of {@link Mapping} to define which field in entities as relevant values.
 */
@RequiredArgsConstructor
public class DefaultHandler {

    /**
     * Entity type which handle this instance.
     */
    @Getter
    private final Class<? extends DefaultEntity> entityClass;

    /**
     * Relevant mapping used to fill entities
     */
    private List<Mapping> entityMappingSelection;

    /**
     * Pre-filled entities to provide when requested
     */
    private List<EntityProcessing> defaultEntities;


    /**
     * Clone a given entity
     *
     * @param entity entity to clone
     * @return a copy of the given entity
     */
    public EntityProcessing clone (DefaultEntity entity) {
        final var clone = EntityHelper.copy(entity, true);

        return EntityProcessing.fromEntity(clone);
    }

    /**
     * Select entity which fit given mapping value.<br>
     * <br>
     * If no entities were registered return an empty one. In the other case, among given mapping select those which were
     * set during entities registration and for the selected mapping find entities were values match.
     *
     * @param entityMappingSelection Map of {@link Mapping} value to select desired entities
     * @return list of entities (can be empty)
     */
    public List<EntityProcessing> getDefaultEntity(Map<Mapping, Object> entityMappingSelection) {
        var selectedEntities = new ArrayList<EntityProcessing>();

        // No default (registered) entities
        if (defaultEntities == null || defaultEntities.isEmpty()) {
            // Add an empty empty (no field set)
            selectedEntities.add(
                    EntityProcessing.fromEntity(
                            createDefaultInstanceFor(entityClass)
                    )
            );
        } else {
            // Among given mapping, select those which were set during entities registration
            final var selection = entityMappingSelection.entrySet()
                    .stream()
                    .filter(entry -> this.entityMappingSelection.contains(entry.getKey()))
                    .toList();

            if (selection.isEmpty()) {
                // No mapping found among givens.
                // Return all registered entities.
                selectedEntities.addAll(defaultEntities);
            } else {
                for (final var entry : selection) {
                    // With selected mappings find entities were value field match
                    var tempSelection = defaultEntities.stream()
                            .filter(entity -> entry.getValue().equals(entity.getPropertyValue(entry.getKey().getTo())))
                            .toList();

                    // If no entity found, check value type and retry when value is a collection
                    if (tempSelection.isEmpty() && entry.getValue() instanceof Collection<?> collection) {
                        tempSelection = defaultEntities.stream()
                                .filter(entity -> collection.contains(entity.getPropertyValue(entry.getKey().getTo())))
                                .toList();
                    }

                    selectedEntities.addAll(tempSelection);
                }
            }
        }

        return selectedEntities;
    }

    /**
     * Register entities with "pre-filled" values and with a list of {@link Mapping} designed which field has relevant values.
     *
     * @param defaultEntities Entities to register
     * @param entityMappingSelection Mapping indicating which fields have relevant value
     */
    public void setDefaultEntities(List<EntityProcessing> defaultEntities, List<Mapping> entityMappingSelection) {
        this.defaultEntities = defaultEntities;
        this.entityMappingSelection = entityMappingSelection;
    }

    /**
     * Create instance for handled entity type
     *
     * @param className entity class type type
     * @return Instance for the given entity type
     * @param <E> entity type
     */
    private <E> E createDefaultInstanceFor(Class<E> className) {
        try {
            return className.getConstructor().newInstance();
        } catch (Exception e) {
            throw new EntityHandlerException("Unable to create default instance for class %s".formatted(className.getName()), e);
        }
    }
}