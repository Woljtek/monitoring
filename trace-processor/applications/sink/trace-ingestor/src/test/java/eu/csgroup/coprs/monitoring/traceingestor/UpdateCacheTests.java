package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.EndTask;
import eu.csgroup.coprs.monitoring.common.datamodel.Header;
import eu.csgroup.coprs.monitoring.common.datamodel.Trace;
import eu.csgroup.coprs.monitoring.common.datamodel.TraceLog;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import eu.csgroup.coprs.monitoring.common.properties.ReloadablePropertySourceEnvironment;
import eu.csgroup.coprs.monitoring.traceingestor.config.IngestionGroup;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(TraceIngestorConfiguration.class)
@ContextConfiguration(initializers = UpdateCacheTests.TestInitializer.class)
@DataJpaTest
// Comment the two below annotation to test with non embedded database
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("dev-embedded")
//@ActiveProfiles("dev-integration")
public class UpdateCacheTests {
    private static final TraceIngestorConfiguration conf = new TraceIngestorConfiguration();

    @Autowired
    private ReloadableBeanFactory factory;

    @Autowired
    private EntityIngestor entityIngestor;

    public static final TemporaryFolder folder = new TemporaryFolder();

    private static final File yamlFile;

    static {
        try {
            folder.create();
            yamlFile = folder.newFile("ingestion_cache.yaml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    ReloadableBeanFactory beanFactory;

    @Test
    public void testConfigUpdate () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var traceLog = new TraceLog();
        final var trace = new Trace();
        trace.setHeader(new Header());
        trace.getHeader().setMission("Mission name");
        trace.getHeader().setRsChainName("Rs chain name");
        trace.setTask(new EndTask());
        traceLog.setTrace(trace);
        final var dsibTrace = new FilteredTrace("IngestionTrigger_Dsib", traceLog);
        sink.accept(toMessage(dsibTrace));

        updateConfigurationFile();

        // When
        sink.accept(toMessage(dsibTrace));

        // Then
        assertThat(entityIngestor.findAll(Dsib.class))
                .allMatch(dsib -> dsib.getFilename().equals("Rs chain name"))
                .allMatch(dsib -> dsib.getMission().equals("Mission name"));

    }

    private void updateConfigurationFile () {
        // Given
        IngestionGroup ingestionGroup;
        final var updatedFile = new File("src/test/resources/ingestion_cacheUpdate.yaml");

        // When
        try {
            Files.copy(updatedFile.toPath(), yamlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Wait for update
        final var start = Instant.now();
        do {
            ingestionGroup = beanFactory.getBean(IngestionGroup.class);
        } while (ingestionGroup.getIngestions().get(0).getMappings().size() != 2
                || Duration.between(Instant.now(), start).toMinutes() > 1);
        beanFactory.getBean(IngestionGroup.class);
    }

    public static class TestInitializer extends ExternalResource implements ApplicationContextInitializer<ConfigurableApplicationContext>, AutoCloseable {
        @Override
        public void close() throws Exception {

        }

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            final var originalFile = new File("src/test/resources/ingestion_cache.yaml");

            try {
                Files.copy(originalFile.toPath(), yamlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final var env = ReloadablePropertySourceEnvironment.getInstance();
            env.setRefreshPeriod(1, TimeUnit.MILLISECONDS);

            try {
                TestPropertyValues.of(
                        "ingestion.path=file:" + yamlFile.getAbsolutePath(),
                        "propertySource.yaml.reload=20"
                ).applyTo(applicationContext.getEnvironment());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Message<FilteredTrace> toMessage(FilteredTrace ref) {
        return new GenericMessage<>(ref);
    }
}
