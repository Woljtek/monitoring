package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.ingestor.EntityMetadata;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Ingestion;
import lombok.Data;

import java.util.*;

@Data
public class ProcessorDescription {
    /**
     * Processor name
     */
    private String name;

    private EntityMetadata entityMetadata;
    /**
     * Key: entity class
     * Value: collection of processor name handling entity creation
     */
    private Map<Class, Collection<String>> relyOnProc = new HashMap<>();

    private Ingestion ingestionConfig;

    public void putRelyOnProcs (Class entityClass, Collection<String> procsName) {
        relyOnProc.put(entityClass, procsName);
    }
}
