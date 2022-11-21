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

import java.lang.management.ManagementFactory;
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
        final var mxBean = ManagementFactory.getThreadMXBean();
        long cpuTimeStart = mxBean.getThreadCpuTime(Thread.currentThread().getId());

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

            final var cpuTimedurationNs = mxBean.getThreadCpuTime(Thread.currentThread().getId()) - cpuTimeStart;
            try (
                    MDC.MDCCloseable ignored = MDC.putCloseable("log_param", ",\"ingestion_duration_in_ms\":%s".formatted(cpuTimedurationNs/1000000))
            ) {
                log.info("Trace ingestion with configuration '%s' done (took %s ms)%n%s".formatted(
                        ingestionConfig.getName(),
                        cpuTimedurationNs/1000000,
                        filteredTrace.getLog()));
            }
        }
    }

    protected final void ingest(TraceLog traceLog, Ingestion ingestionConfig) {
        // Create entity instance from trace instance based on ingestionConfig rules.
        try {
            final var beanAccessor = BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(traceLog));

            final var processDescs = new DescriptorBuilder(ingestionConfig).build();

            final var orchestrator = new ProcessorOrchestrator();
            orchestrator.setBeanAccessor(beanAccessor);
            orchestrator.setProcessorDescriptions(processDescs);
            orchestrator.setIngestionConfig(ingestionConfig);

            entityIngestor.process(orchestrator);
        } catch (Exception e) {
            final var errorMessage = "Error occurred ingesting trace with configuration '%s'%n%s: ".formatted(ingestionConfig.getName(), traceLog);
            log.error(errorMessage, e);
            throw new IngestionException(errorMessage, e);
        }
    }


}
