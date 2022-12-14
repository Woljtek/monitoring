package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.TraceLog;
import eu.csgroup.coprs.monitoring.common.ingestor.DataBaseIngestionTimer;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityStatistics;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import eu.csgroup.coprs.monitoring.traceingestor.config.Ingestion;
import eu.csgroup.coprs.monitoring.traceingestor.config.IngestionGroup;
import eu.csgroup.coprs.monitoring.traceingestor.processor.DescriptorBuilder;
import eu.csgroup.coprs.monitoring.traceingestor.processor.ProcessorDescription;
import eu.csgroup.coprs.monitoring.traceingestor.processor.ProcessorOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.messaging.Message;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


@Slf4j
public class TraceIngestorSink implements Consumer<Message<FilteredTrace>> {

    private final ReloadableBeanFactory reloadableBeanFactory;

    private final EntityIngestor entityIngestor;

    private final Map<String, ProcessorDescriptionConfig> cache = new HashMap<>();


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
        final var ingestionGroupConfig = reloadableBeanFactory.getBean(IngestionGroup.class);

        final var processorDescriptionConfig = selectProcessorDescriptor(ingestionGroupConfig, filteredTrace);

        ingest(filteredTrace.getLog(), processorDescriptionConfig);

        final var cpuTimedurationNs = mxBean.getThreadCpuTime(Thread.currentThread().getId()) - cpuTimeStart;
        try (
                MDC.MDCCloseable ignored = MDC.putCloseable("log_param", ",\"ingestion_duration_in_ms\":%s".formatted(cpuTimedurationNs/1000000))
        ) {
            log.info("Trace ingestion with configuration '%s' done (took %s ms)%n%s".formatted(
                    processorDescriptionConfig.ingestionConfig.getName(),
                    cpuTimedurationNs/1000000,
                    filteredTrace.getLog()));
        }
    }

    /**
     * Select corresponding {@link ProcessorDescriptionConfig} in cache if any set or create one from zero. <br>
     * <br>
     * if associated ingestion configuration set in cache is not the same as the one retrieved from configuration,
     * it implies that the configuration has changed and so cache is cleared to take into account new configuration.
     *
     * @param ingestionGroupConfig configuration
     * @param filteredTrace trace for which to select the right ingestion config and so processor to execute.
     * @return {@link ProcessorDescriptionConfig} instance.
     */
    private ProcessorDescriptionConfig selectProcessorDescriptor (IngestionGroup ingestionGroupConfig, FilteredTrace filteredTrace) {
        final var ingestionStrategy = ingestionGroupConfig.getIngestions()
                .stream()
                .filter(m -> m.getName().equals(filteredTrace.getRuleName()))
                .findFirst();

        if (ingestionStrategy.isEmpty()) {
            String errorMessage = "No configuration found for '%s'%n%s".formatted(filteredTrace.getRuleName(), filteredTrace.getLog());
            log.error(errorMessage);
            throw new IngestionException(errorMessage);
        }

        final var currentIngestionConfig = ingestionStrategy.get();

        ProcessorDescriptionConfig processorDescriptionConfig = null;
        // Check in cache first
        if (cache.containsKey(currentIngestionConfig.getName())) {
            processorDescriptionConfig = cache.get(currentIngestionConfig.getName());

            // Check if configuration was not changed
            // If so clear cache
            if (! currentIngestionConfig.equals(processorDescriptionConfig.ingestionConfig)) {
                cache.clear();
                processorDescriptionConfig = null;
            }
        }

        if (processorDescriptionConfig == null) {
            // Otherwise create processor description config
            processorDescriptionConfig = new ProcessorDescriptionConfig(
                    currentIngestionConfig,
                    new DescriptorBuilder(currentIngestionConfig).build()
            );

            cache.put(currentIngestionConfig.getName(), processorDescriptionConfig);
        }

        return processorDescriptionConfig;
    }




    protected final void ingest (TraceLog traceLog, ProcessorDescriptionConfig processorDescriptionConfig) {
        try {
            final var beanAccessor = BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(traceLog));

            final var orchestrator = new ProcessorOrchestrator();
            orchestrator.setBeanAccessor(beanAccessor);
            orchestrator.setProcessorDescriptions(processorDescriptionConfig.processorDescriptions);
            orchestrator.setIngestionConfig(processorDescriptionConfig.ingestionConfig);

            entityIngestor.process(orchestrator);
        } catch (Exception e) {
            final var errorMessage = "Error occurred ingesting trace with configuration '%s'%n%s: ".formatted(
                    processorDescriptionConfig.ingestionConfig.getName(),
                    traceLog);
            log.error(errorMessage, e);
            throw new IngestionException(errorMessage, e);
        }
    }


    private record ProcessorDescriptionConfig(
        Ingestion ingestionConfig,
        Collection<ProcessorDescription> processorDescriptions
    ) {

    }
}