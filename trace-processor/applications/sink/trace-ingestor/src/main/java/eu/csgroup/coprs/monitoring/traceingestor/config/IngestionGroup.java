package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.properties.ReloadableYamlPropertySourceFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;


/**
 * Configuration class to group set of {@link Ingestion} configuration
 */
@Data
@Configuration
@PropertySource(name = "ingestionGroup", value = "${ingestion.path}", factory = ReloadableYamlPropertySourceFactory.class)
@ConfigurationProperties
@Slf4j
public class IngestionGroup {
    /**
     * Ingestion group
     */
    List<Ingestion> ingestions;
}
