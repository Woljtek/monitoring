package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.datamodel.Trace;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import eu.csgroup.coprs.monitoring.traceingestor.processor.DefaultProcessor;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Ingestion;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.IngestionGroup;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Slf4j
public class TraceIngestorSink implements Consumer<Message<FilteredTrace>> {

    private static final Logger LOGGER = getLogger(TraceIngestorSink.class);

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

        ingestionStrategy.ifPresentOrElse(
                m -> ingest(filteredTrace.getTrace(), m),
                () -> {
                    String errorMessage = "No configuration found for '%s'\n%s".formatted(filteredTrace.getRuleName(), filteredTrace.getTrace());
                    log.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
        );

        final var duration = Duration.between(start, Instant.now());
        try (MDC.MDCCloseable mdc = MDC.putCloseable("log_param", ",\"ingestion_duration_in_ms\":%s".formatted(duration.toMillis()))) {
            log.info("Trace ingestion with configuration '%s' done (took %s ms)\n%s".formatted(
                    ingestionStrategy.get().getName(),
                    duration.toMillis(),
                    filteredTrace.getTrace()));
        }
    }

    protected final <T extends ExternalInput> void ingest(Trace trace, Ingestion mapping) {
        // Create entity instance from trace instance based on mapping rules.
        try {
            final var entityDependencies = mapping.getDependencies()
                    .stream()
                    .collect(Collectors.groupingBy(BeanProperty::getBeanName));

            final Function<Mapping, String> groupFunc = m -> m.entityPath().getBeanName();

            final var entityMappings = mapping.getMappings()
                    .stream()
                    .collect(Collectors.groupingBy(groupFunc));

            final var beanAccessor = BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(trace));

            entityDependencies.entrySet()
                    .stream()
                    .map(entry -> createProcessor(entry.getKey(), mapping.getName(), entityMappings.get(entry.getKey()), entry.getValue()))
                    .map(processor -> (Supplier<List<ExternalInput>>) () -> processor.apply(beanAccessor))
                    .map(entityIngestor::process)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            final var errorMessage = "Error occurred ingesting trace with configuration '%s'\n%s: ".formatted(mapping.getName(), trace);
            log.error("%s\n%s".formatted(errorMessage, e.getMessage()));
            throw new RuntimeException(errorMessage, e);
        }
    }

    private <T extends ExternalInput> DefaultProcessor<T> createProcessor(String entityName, String configurationName, List<Mapping> mappings, List<BeanProperty> dependencies) {
        try {
            final var className = Class.forName("%s.%sProcessor".formatted(DefaultProcessor.class.getPackageName(), entityName));
            return (DefaultProcessor<T>) className.getConstructor(String.class, String.class, List.class, List.class, EntityFinder.class)
                    .newInstance(entityName, configurationName, mappings, dependencies, entityIngestor);
        } catch (Exception e) {
            return new DefaultProcessor<T>(entityName, configurationName, mappings, dependencies, entityIngestor);
        }
    }
}
