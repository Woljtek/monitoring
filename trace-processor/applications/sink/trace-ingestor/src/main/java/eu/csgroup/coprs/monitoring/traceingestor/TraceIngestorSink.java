package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
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
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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
        final var filteredTrace = message.getPayload();
        // Find mapping associated to filter name
        final var ingestionStrategy = reloadableBeanFactory.getBean(IngestionGroup.class).getIngestions()
                .stream()
                .filter(m -> m.getName().equals(filteredTrace.getRuleName()))
                .findFirst();

        ingestionStrategy.ifPresentOrElse(
                m -> ingest(filteredTrace.getTrace(), m),
                () -> {
                    throw new RuntimeException("No configuration found for '%s'".formatted(filteredTrace.getRuleName()));
                }
        );

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
                    .map(entry -> createProcessor(entry.getKey(), entityMappings.get(entry.getKey()), entry.getValue()))
                    .map(processor -> processor.apply(beanAccessor))
                    .map(entityIngestor::saveAll)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private <T extends ExternalInput> DefaultProcessor<T> createProcessor(String entityName, List<Mapping> mappings, List<BeanProperty> dependencies) {
        final BiFunction<Specification<? extends ExternalInput>, Class<? extends ExternalInput>, List<? extends ExternalInput>> entityFinder = entityIngestor::findAll;
        try {
            final var className = Class.forName("%s.%sProcessor".formatted(DefaultProcessor.class.getPackageName(), entityName));
            return (DefaultProcessor<T>) className.getConstructor(String.class, List.class, List.class, BiFunction.class)
                    .newInstance(entityName, mappings, dependencies, entityFinder);
        } catch (Exception e) {
            return new DefaultProcessor<T>(entityName, mappings, dependencies, entityFinder);
        }
    }
}
