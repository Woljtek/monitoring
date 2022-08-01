package eu.csgroup.coprs.monitoring.traceingestor.mapping;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Vector;

@Data
public class Ingestion {
    public static final String TRACE_PREFIX = "trace.%s";

    private String name;
    private List<Mapping> mappings;
    private List<BeanProperty> dependencies;

    public void setMappings(Map<String, String> mappings) {
        this.mappings = new Vector<>();
        mappings.entrySet().stream().forEach(entry -> {
            this.mappings.add(Mapping.from(TRACE_PREFIX.formatted(entry.getKey()), entry.getValue()));
        });
    }
}
