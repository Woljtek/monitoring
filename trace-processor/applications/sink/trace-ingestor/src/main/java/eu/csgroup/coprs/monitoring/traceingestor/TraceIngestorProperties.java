package eu.csgroup.coprs.monitoring.traceingestor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ingestion")
public class TraceIngestorProperties {
    /**
     * Configuration file path containing strategy to ingest trace. Must be an absolute path prefixed with 'file:'
     */
    private String path;
}
