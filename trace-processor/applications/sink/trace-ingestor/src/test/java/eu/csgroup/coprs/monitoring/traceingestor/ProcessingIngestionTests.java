package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.*;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(TraceIngestorConfiguration.class)
@ContextConfiguration(initializers = TestInitializer.class)
@DataJpaTest
// Comment the two below annotation to test with non embedded database
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("dev-embedded")
//@ActiveProfiles("dev-integration")
public class ProcessingIngestionTests {

    private static final TraceIngestorConfiguration conf = new TraceIngestorConfiguration();

    @Autowired
    private ReloadableBeanFactory factory;

    @Autowired
    private EntityIngestor entityIngestor;


    @After
    public void setUpAfterTest () {
        entityIngestor.deleteAll();
    }

    @Test
    public void testNominal () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var processingRef = getProcessingRef("processing_all");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(Dsib.class))
                .hasSize(2);
        assertThat(entityIngestor.findAll(Chunk.class))
                .hasSize(6);
        assertThat(entityIngestor.findAll(AuxData.class))
                .hasSize(3);
        assertThat(entityIngestor.findAll(InputListExternal.class))
                    .hasSize(9);
        assertThat(entityIngestor.findAll(Processing.class))
                .hasSize(1);
        assertThat(entityIngestor.findAll(Product.class))
                .hasSize(6);
        assertThat(entityIngestor.findAll(InputListInternal.class))
                .hasSize(2);
        assertThat(entityIngestor.findAll(OutputList.class))
                .hasSize(4);
        assertThat(entityIngestor.findAll(MissingProducts.class))
                .hasSize(0);
    }

    @Test
    public void testWithMissingOutput () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var processingRef = getProcessingRefWithMissingProducts("processing_all");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        final var processing = entityIngestor.findAll(Processing.class).get(0);
        assertThat(entityIngestor.findAll(MissingProducts.class))
                .hasSize(3)
                .extracting(MissingProducts::getProcessing)
                .extracting(Processing::getId)
                .allMatch(processingFailedId -> processingFailedId.equals(processing.getId()));
    }

    @Test
    public void testExistingEntity () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var processingRef = getProcessingRef("processing_all");

        final var dsib = new Dsib();
        dsib.setFilename("DCS_05_S2B_20210927072424023820_ch1_DSIB.xml");

        entityIngestor.process((ei) -> List.of(
                dsib
        ));

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(Chunk.class))
                .isNotEmpty();
    }

    @Test
    public void testInputOnly () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var processingRef = getProcessingRef("processing_input");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(Product.class))
                .hasSize(0);
    }

    @Test
    public void testOutputOnly () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var processingRef = getProcessingRef("processing_output");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(ExternalInput.class))
                .hasSize(0);
    }


    // -- Helper -- //
    private FilteredTrace getProcessingRefWithMissingProducts (String filterName) {
        final var ref = getProcessingRef(filterName);

        final var missingOutput1 = new HashMap<String, Object>();
        missingOutput1.put("estimated_count_integer", 8);
        missingOutput1.put("end_to_end_product", true);
        missingOutput1.put("product_metadata_custom_object", List.of(Map.of("pmco1", "value1"), Map.of("pmco2", "value2")));

        final var missingOutput2 = new HashMap<String, Object>();
        missingOutput2.put("estimated_count_integer", 2);
        missingOutput2.put("end_to_end_product", false);
        missingOutput2.put("product_metadata_custom_object", List.of(Map.of("pmco3", "value3")));

        ((EndTask)(ref.getLog().getTrace().getTask())).setMissingOutput(List.of(missingOutput1, missingOutput2));

        return ref;
    }
    private FilteredTrace getProcessingRef (String filterName) {
        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setMission("S2");
        header.setLevel(Level.INFO);
        header.setWorkflow(Workflow.NOMINAL);

        final var task = new EndTask();
        //task.setSatellite("S2B");

        final var output = new HashMap<>();
        output.put("filename_strings", List.of(
                "GS2B_20170322T000000_013601_N02.01",
                "GS2B_20170322T000000_013601_N02.02.zip",
                "GS2B_20170322T000000_013601_N02.03.zip",
                "GS2B_20170322T000000_013601_N02.04")
        );
        task.setOutput(output);

        final var input = new HashMap<>();
        input.put("filename_strings", List.of(
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00002.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00003.raw",
                "DCS_05_S2B_20210927072424023813_ch2_DSDB_00001.raw",
                "DCS_05_S2B_20210927072424023813_ch2_DSDB_00002.raw",
                "DCS_05_S2B_20210927072424023813_ch2_DSDB_00003.raw",
                "S1A_OPER_AMH_ERRMAT_W.XML",
                "S2A_OPER_AUX_TEST_TE",
                "S3A_OL_0_TESTAX_12345678T123456_12345678T123456_12345678T123456___________________123_12345678.SEN3",
                "GS2B_20170322T000000_013601_N02.05",
                "GS2B_20170322T000000_013601_N02.06.zip")
        );
        task.setInput(input);

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setTask(task);

        final var custom = new HashMap<>();
        custom.put("key1", "value1");
        custom.put("key2", "value2");
        custom.put("key3", "value3");
        trace.setCustom(custom);

        final var traceLog = new TraceLog();
        traceLog.setTrace(trace);

        return new FilteredTrace(filterName, traceLog);
    }

    private Message<FilteredTrace> toMessage(FilteredTrace ref) {
        return new GenericMessage<>(ref);
    }
}
