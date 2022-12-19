package eu.csgroup.coprs.monitoring.traceingestor.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Configuration class to restrict relation on multiple entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Alias {
    /**
     * The entity for which to restrict relation
     */
    private String entity;
    /**
     * Restrict relation to the following entity
     */
    private String restrict;
}
