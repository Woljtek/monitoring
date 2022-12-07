package eu.csgroup.coprs.monitoring.traceingestor.entity;

/**
 * Indicate the state of the processed entity.<br>
 * <br>
 * Principally used to identify if the entity must be stored or not.
 */
public enum EntityState {
    /**
     * Used when entity is freshly created
     */
    NEW,
    /**
     * Used when at least one field of the entity is updated (old != new)
     */
    UPDATED,
    /**
     * Used when no field was updated
     */
    UNCHANGED
}
