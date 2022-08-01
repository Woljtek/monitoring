package eu.csgroup.coprs.monitoring.traceingestor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "trace-ingestor")
public class TraceIngestorProperties {

}
