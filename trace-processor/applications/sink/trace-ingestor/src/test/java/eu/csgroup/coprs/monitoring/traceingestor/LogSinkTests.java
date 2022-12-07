package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.AutoMergeableMap;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.datamodel.*;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.*;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@Import(TraceIngestorConfiguration.class)
@ContextConfiguration(initializers = TestInitializer.class)
@DataJpaTest
// Comment the two below annotation to test with non embedded database
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("dev-embedded")
//@ActiveProfiles("dev-integration")
public class LogSinkTests {

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
        final var dsibRef = getDsibRef();

        // When
        sink.accept(toMessage(dsibRef));

        // Then
        assertThat(entityIngestor.findAll(Dsib.class))
                .hasSize(1)
                .allMatch(Objects::nonNull);
    }

    @Test
    public void testMappingDsibFirst() {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();
        final var chunkRef = getChunkRef();
        final var auxDataRef = getAuxDataRef();
        final var productRef = getProductRef();

        // When
        sink.accept(toMessage(dsibRef));
        sink.accept(toMessage(chunkRef));
        sink.accept(toMessage(auxDataRef));
        sink.accept(toMessage(productRef));

        // Then
        assertThat(entityIngestor.findAll(ExternalInput.class))
                .hasSize(3)
                .allMatch(e -> e.getFilename() != null)
                .allMatch(e -> e.getMission() != null)
                .allMatch(e -> e.getIngestionDate() != null)
                .allMatch(e -> e.getCatalogStorageEndDate() != null)
                .allMatch(e -> e.getAvailableDate() != null)
                .allMatch(e -> e.getSeenDate() != null);
        assertThat(entityIngestor.findAll(Product.class))
                .hasSize(1)
                .allMatch(p -> p.getFilename() != null)
                .allMatch(p -> p.getTimelinessName() != null)
                .allMatch(p -> p.getTimelinessValueSeconds() != 0)
                .allMatch(Product::isEndToEndProduct)
                .allMatch(p -> !p.getCustom().isEmpty());
        assertThat(entityIngestor.findAll(InputListInternal.class))
                .isEmpty();
        assertThat(entityIngestor.findAll(OutputList.class))
                .isEmpty();
    }

    @Test
    public void testMappingChunkFirst() {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();
        final var chunkRef = getChunkRef();
        final var auxDataRef = getAuxDataRef();

        // When
        sink.accept(toMessage(chunkRef));
        sink.accept(toMessage(dsibRef));
        sink.accept(toMessage(auxDataRef));

        // Then
        assertThat(entityIngestor.findAll(ExternalInput.class))
                .hasSize(3)
                .allMatch(e -> e.getFilename() != null)
                .allMatch(e -> e.getMission() != null)
                .allMatch(e -> e.getIngestionDate() != null)
                .allMatch(e -> e.getCatalogStorageEndDate() != null)
                .allMatch(e -> e.getAvailableDate() != null)
                .allMatch(e -> e.getSeenDate() != null);
    }

    @Test
    public void testOneToManyMapping() {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var chunksRef = getMultiChunkRef(
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00002.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00004.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00005.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00006.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00007.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00008.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00009.raw",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00010.raw"
        );

        // When
        sink.accept(toMessage(chunksRef));

        // Then
        assertThat(entityIngestor.findAll(Dsib.class))
                .hasSize(1);
        assertThat(entityIngestor.findAll(Chunk.class))
                .hasSize(9);
        assertThat(entityIngestor.findAll(ExternalInput.class))
                .hasSize(10);
    }

    @Test
    public void testUnicityClause () {
        final var sink = conf.traceIngestor(factory, entityIngestor);
        // Given
        final var dsibRef = getDsibRef();

        // When
        sink.accept(toMessage(dsibRef));
        sink.accept(toMessage(dsibRef));

        // Then
        assertThat(entityIngestor.findAll(Dsib.class))
                .hasSize(1);
    }

    @Test
    public void testEntityUpdate () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();
        final var filename = ((EndTask) dsibRef.getLog().getTrace().getTask()).getOutput().get("filename").toString();
        final var oldMissionRef = dsibRef.getLog().getTrace().getHeader().getMission();

        // When
        sink.accept(toMessage(dsibRef));
        dsibRef.getLog().getTrace().getHeader().setMission("S1");
        ((EndTask) dsibRef.getLog().getTrace().getTask()).getOutput().put("channel_id", "ch_2");
        sink.accept(toMessage(dsibRef));

        // Then
        assertThat(entityIngestor.findEntityBy(Dsib.class, Map.of("filename", filename)))
                .extracting("mission")
                .isNotEqualTo(oldMissionRef);
    }

    @Test
    public void testMapEntityUpdate () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var auxDataRef = getAuxDataRef();
        final var output = ((EndTask) auxDataRef.getLog().getTrace().getTask()).getOutput();

        final var outputUpdate = Map.of(
                "test_string", "string10",
                "test_strings", List.of("string1", "string2", "string4"),
                "test_object", Map.of("object1", "value10", "object2", "value2", "object4", "value4"),
                "filename", output.get("filename")
        );

        final var customUpdate = Map.of(
                "test_field_string", "test_value",
                "test_field_strings", List.of("test_value1", "test_value2"),
                "test_field_object", Map.of("test_object1", "test_value1", "test_object3", "test_value3")
        );

        // When
        sink.accept(toMessage(auxDataRef));
        ((EndTask) auxDataRef.getLog().getTrace().getTask()).setOutput(outputUpdate);
        auxDataRef.getLog().getTrace().setCustom(customUpdate);
        sink.accept(toMessage(auxDataRef));

        // Then
        output.put("test_string", outputUpdate.get("test_string"));
        final var collection = new HashSet<Object>((Collection<Object>)output.get("test_strings"));
        collection.add("string2");
        collection.add("string4");
        output.put("test_strings", collection);
        ((Map<String, Object>)(output.get("test_object"))).put("object1", "value10");
        ((Map<String, Object>)(output.get("test_object"))).put("object2", "value2");
        ((Map<String, Object>)(output.get("test_object"))).put("object4", "value4");
        final var custom = new AutoMergeableMap();
        custom.put("destination", output);
        custom.putAll(customUpdate);

        assertThat(entityIngestor.findAll(AuxData.class))
                .hasSize(1)
                .element(0)
                .matches(t -> {
                    System.out.println(t);
                    return true;
                })
                .extracting(AuxData::getCustom)
                .isEqualTo(custom);
    }

    @Test
    public void testMissingRule () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var fTrace = new FilteredTrace("test", new TraceLog());

        // When
        final var assertThrown = assertThatThrownBy(() -> sink.accept(toMessage(fTrace)));

        // Then
        assertThrown.isNotNull()
                .hasMessage("No configuration found for 'test'\n%s".formatted(fTrace.getLog()));
    }

    @Test
    public void testFindEntityByField () {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();
        final var filename = ((EndTask)dsibRef.getLog().getTrace().getTask()).getOutput().get("filename").toString();

        // When
        sink.accept(toMessage(dsibRef));
        sink.accept(toMessage(dsibRef));

        // Then
        assertThat(entityIngestor.findEntityBy(Dsib.class, Map.of("filename", filename)))
                .isNotEmpty()
                .hasSize(1);
    }


    // -- Helper -- //

    private FilteredTrace getDsibRef() {
        var ref = getRawRef();
        ref.getTrace().getTask().setName("DSIB Trace");
        ((EndTask)ref.getTrace().getTask()).getOutput().put("filename", "DCS_05_S2B_20210927072424023813_ch1_DSIB.xml");

        return new FilteredTrace("dsib", ref);
    }

    private FilteredTrace getChunkRef() {
        var ref = getRawRef();
        ref.getTrace().getTask().setName("CHUNK Trace");
        ((EndTask)ref.getTrace().getTask()).getOutput().put("filename", "DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw");

        return new FilteredTrace("chunk", ref);
    }

    private FilteredTrace getMultiChunkRef(String ... additionalChunks) {
        var ref = getRawRef();
        ref.getTrace().getTask().setName("CHUNK Trace");
        final var filenames = new ArrayList<>(Arrays.asList(additionalChunks));

        ((EndTask)ref.getTrace().getTask()).getOutput().put("filename", filenames);

        return new FilteredTrace("chunk", ref);
    }

    private FilteredTrace getAuxDataRef() {
        var ref = getRawRef();
        ref.getTrace().getTask().setName("AUX_DATA Trace");
        ((EndTask)ref.getTrace().getTask()).getOutput().put("filename", "S2B_OPER_GIP_R2EQOG_MPC__20220315T151100_V20220317T000000_21000101T000000_B10");

        return new FilteredTrace("aux_data", ref);
    }

    private TraceLog getRawRef () {
        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setMission("S2");

        final var task = new EndTask();
        //task.setSatellite("S2B");

        final var output = new HashMap<String, Object>();
        output.put("pickup_point_seen_date", "2019-01-21T05:24:40.000000Z");
        output.put("pickup_point_available_date", "2019-01-21T05:24:40.000000Z");
        output.put("ingestion_date", "2019-01-21T05:24:40.000000Z");
        output.put("catalog_storage_date", "2019-01-21T05:24:40.000000Z");
        output.put("channel_id", "ch_1");
        output.put("station", "XBIP");
        output.put("test_strings", new ArrayList<>(List.of("string1", "string3")));
        output.put("test_string", "string10");
        output.put("test_object", new HashMap<>(Map.<String, Object>of("object1", "value1", "object3", "value3")));
        task.setOutput(output);

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setTask(task);

        final var custom = new HashMap<String, Object>();
        custom.put("key1", "value1");
        custom.put("key2", "value2");
        custom.put("key3", "value3");
        trace.setCustom(custom);

        final var traceLog = new TraceLog();
        traceLog.setTrace(trace);

        return traceLog;
    }

    private FilteredTrace getProductRef() {
        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setMission("S2");

        final var task = new EndTask();

        final var output = new HashMap<String, Object>();
        output.put("timeliness_name_string", "timeliness test");
        output.put("timeliness_value_seconds_integer", 100);
        output.put("end_to_end_product_boolean", true);
        output.put("product_metadata_custom_object", Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        ));
        task.setOutput(output);

        final var input = new HashMap<String, Object>();
        input.put("filename_string", "S2B_OPER_MSI_L0__DS_2BPS_20220529T143730_S20220529T002607_N04");
        task.setInput(input);

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setTask(task);

        final var traceLog = new TraceLog();
        traceLog.setTrace(trace);

        return new FilteredTrace("product", traceLog);
    }

    private Message<FilteredTrace> toMessage(FilteredTrace ref) {
        return new GenericMessage<>(ref);
    }
}