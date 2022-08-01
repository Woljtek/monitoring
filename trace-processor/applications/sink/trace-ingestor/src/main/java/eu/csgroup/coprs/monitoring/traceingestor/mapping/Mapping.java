package eu.csgroup.coprs.monitoring.traceingestor.mapping;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;


public record Mapping (
        BeanProperty tracePath,
        BeanProperty entityPath
) {
    public static Mapping from(String tracePath, String entityPath) {
        return new Mapping(
                new BeanProperty(tracePath),
                new BeanProperty(entityPath)
        );
    }
}
