package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityMetadata;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
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
    private Map<Class<? extends DefaultEntity>, Collection<String>> relyOnProc = new HashMap<>();

    private List<Mapping> mappings;

    public void putRelyOnProcs (Class<? extends DefaultEntity> entityClass, Collection<String> procsName) {
        relyOnProc.put(entityClass, procsName);
    }
}