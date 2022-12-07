package eu.csgroup.coprs.monitoring.traceingestor.config;

import lombok.Data;


/**
 * Configuration class to set an alias to an object
 *
 * @param <T> Object type to which to set an alias
 */
@Data
public class AliasWrapper<T> {
    private final String alias;

    private final T wrappedObject;
}
