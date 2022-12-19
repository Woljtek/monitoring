package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class to define a set of mapping to create entities from a trace.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Ingestion {
    /**
     * Configuration name
     */
    private String name;
    /**
     * Mapping list to define where trace value must be set in which entity and field
     */
    private List<Mapping> mappings;
    /**
     * Map where key define the alias name and value define restriction on which entity
     */
    private Map<String, Alias> alias = new HashMap<>();

    /**
     * List of SQL query to use under certain condition to process duplicate processing.
     */
    private List<DuplicateProcessing> duplicateProcessings;

    /**
     * Set alias by modifying key to be in pascal case
     *
     * @param associations Alias mapping
     */
    public void setAlias(Map<String, Alias> associations) {
        this.alias = associations.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> PropertyUtil.snake2PascalCasePropertyName(entry.getKey()),
                        Map.Entry::getValue)
                );
    }
}
