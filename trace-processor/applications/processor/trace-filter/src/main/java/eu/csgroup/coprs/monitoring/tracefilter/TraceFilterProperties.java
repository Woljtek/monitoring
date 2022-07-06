package eu.csgroup.coprs.monitoring.tracefilter;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "filter")
public class TraceFilterProperties {
    /**
     * Configuration file path containing rules to filter trace. Must be an absolute path prefixed with 'file:'
     */
    private String path;
}
