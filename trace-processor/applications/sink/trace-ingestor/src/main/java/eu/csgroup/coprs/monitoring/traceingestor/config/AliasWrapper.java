package eu.csgroup.coprs.monitoring.traceingestor.config;

import lombok.Data;

@Data
public class AliasWrapper<T> {
    private final String alias;

    private final T wrappedObject;
}
