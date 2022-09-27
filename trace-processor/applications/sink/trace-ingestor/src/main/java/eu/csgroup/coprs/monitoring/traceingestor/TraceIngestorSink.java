package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.TraceLog;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import eu.csgroup.coprs.monitoring.traceingestor.config.Ingestion;
import eu.csgroup.coprs.monitoring.traceingestor.config.IngestionGroup;
import eu.csgroup.coprs.monitoring.traceingestor.processor.DescriptorBuilder;
import eu.csgroup.coprs.monitoring.traceingestor.processor.ProcessorOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;


@Slf4j
public class TraceIngestorSink implements Consumer<Message<FilteredTrace>> {

    private final ReloadableBeanFactory reloadableBeanFactory;

    private final EntityIngestor entityIngestor;


    public TraceIngestorSink(ReloadableBeanFactory reloadableBeanFactory, EntityIngestor entityIngestor) {
        this.reloadableBeanFactory = reloadableBeanFactory;
        this.entityIngestor = entityIngestor;
    }

    @Override
    public void accept(Message<FilteredTrace> message) {
        final Instant start = Instant.now();

        final var filteredTrace = message.getPayload();
        // Find mapping associated to filter name
        final var ingestionStrategy = reloadableBeanFactory.getBean(IngestionGroup.class).getIngestions()
                .stream()
                .filter(m -> m.getName().equals(filteredTrace.getRuleName()))
                .findFirst();

        if (ingestionStrategy.isEmpty()) {
            String errorMessage = "No configuration found for '%s'%n%s".formatted(filteredTrace.getRuleName(), filteredTrace.getLog());
            log.error(errorMessage);
            throw new IngestionException(errorMessage);
        } else {
            final var ingestionConfig = ingestionStrategy.get();
            ingest(filteredTrace.getLog(), ingestionConfig);

            final var duration = Duration.between(start, Instant.now());
            try (MDC.MDCCloseable ignored = MDC.putCloseable("log_param", ",\"ingestion_duration_in_ms\":%s".formatted(duration.toMillis()))) {
                log.info("Trace ingestion with configuration '%s' done (took %s ms)%n%s".formatted(
                        ingestionConfig.getName(),
                        duration.toMillis(),
                        filteredTrace.getLog()));
            }
        }
    }

    protected final void ingest(TraceLog traceLog, Ingestion mapping) {
        // Create entity instance from trace instance based on mapping rules.
        try {
            final var beanAccessor = BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(traceLog));

            final var processDescs = new DescriptorBuilder(mapping).build();

            final var orchestrator = new ProcessorOrchestrator();
            orchestrator.setBeanAccessor(beanAccessor);
            orchestrator.setProcessorDescriptions(processDescs);

            entityIngestor.process(orchestrator);
        } catch (Exception e) {
            final var errorMessage = "Error occurred ingesting trace with configuration '%s'%n%s: ".formatted(mapping.getName(), traceLog);
            log.error(errorMessage, e);
            throw new IngestionException(errorMessage, e);
        }
    }


}
