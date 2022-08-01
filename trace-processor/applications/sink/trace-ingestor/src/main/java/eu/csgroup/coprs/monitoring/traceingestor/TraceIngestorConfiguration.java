package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.IngestionGroup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@EnableConfigurationProperties({ TraceIngestorProperties.class, IngestionGroup.class })
@Configuration
@Import({ EntityIngestor.class, ReloadableBeanFactory.class })
public class TraceIngestorConfiguration {

    @Bean(name = "trace-ingestor")
    public TraceIngestorSink traceIngestor(ReloadableBeanFactory reloadableBeanFactory, EntityIngestor entityIngestor) {
        return new TraceIngestorSink(reloadableBeanFactory, entityIngestor);
    }

}
