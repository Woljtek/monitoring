package eu.csgroup.coprs.monitoring.traceingestor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.datamodel.*;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.AuxData;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@Import(TraceIngestorConfiguration.class)
@ContextConfiguration(initializers = TestInitializer.class)
@DataJpaTest
@Transactional
@ActiveProfiles("dev-embedded")
//@ActiveProfiles("dev-integration")
public class LogSinkTests {

    private static final TraceIngestorConfiguration conf = new TraceIngestorConfiguration();

    @Autowired
    private ReloadableBeanFactory factory;

    @Autowired
    private EntityIngestor entityIngestor;

    @Test
    public void testNominal () throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();

        // When
        sink.accept(toMessage(dsibRef));

        // Then
        assertThat(entityIngestor.findAll(Dsib.class))
                .hasSize(1)
                .allMatch(entity -> entity instanceof Dsib);
    }

    @Test
    public void testMappingDsibFirst() throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();
        final var chunkRef = getChunkRef();
        final var auxDataRef = getAuxDataRef();

        // When
        sink.accept(toMessage(dsibRef));
        sink.accept(toMessage(chunkRef));
        sink.accept(toMessage(auxDataRef));

        // Then
        assertThat(entityIngestor.list())
                .hasSize(3)
                .allMatch(e -> e.getFilename() != null)
                .allMatch(e -> e.getMission() != null)
                .allMatch(e -> e.getIngestionDate() != null)
                .allMatch(e -> e.getCatalogStorageDate() != null)
                .allMatch(e -> e.getPickupPointAvailableDate() != null)
                .allMatch(e -> e.getPickupPointSeenDate() != null);
    }

    @Test
    public void testMappingChunkFirst() throws Exception {
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
        assertThat(entityIngestor.list())
                .hasSize(3)
                .allMatch(e -> e.getFilename() != null)
                .allMatch(e -> e.getMission() != null)
                .allMatch(e -> e.getIngestionDate() != null)
                .allMatch(e -> e.getCatalogStorageDate() != null)
                .allMatch(e -> e.getPickupPointAvailableDate() != null)
                .allMatch(e -> e.getPickupPointSeenDate() != null);
    }

    @Test
    public void testOneToManyMapping() throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var chunksRef = getMultiChunkRef();

        // When
        sink.accept(toMessage(chunksRef));

        // Then
        assertThat(entityIngestor.list())
                .hasSize(10);
    }

    @Test
    public void testGlobalTransaction() throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var chunksRef = getMultiChunkRef();
        final var duplicateChunk = new Chunk();
        duplicateChunk.setFilename("DCS_05_S2B_20210927072424023813_ch1_DSDB_00008.raw");

        // When
        entityIngestor.saveAll(List.of(duplicateChunk));

        // Then
        assertThatThrownBy(() -> sink.accept(toMessage(chunksRef))).isNotNull();
        assertThat(entityIngestor.list())
                .hasSize(2); // Duplicate chunk plus associated dsib
    }


    @Test
    public void testUnicityClause () throws Exception {
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
    public void testEntityUpdate () throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();
        final var filename = ((EndTask) dsibRef.getTrace().getTask()).getOutput().get("filename").toString();
        final var oldMissionRef = dsibRef.getTrace().getHeader().getMission();

        // When
        sink.accept(toMessage(dsibRef));
        dsibRef.getTrace().getHeader().setMission("S1");
        ((EndTask) dsibRef.getTrace().getTask()).getOutput().put("channel_id", "ch_2");
        sink.accept(toMessage(dsibRef));

        // Then
        assertThat(entityIngestor.findEntityBy(Map.of("filename", filename)))
                .extracting("mission")
                .isNotEqualTo(oldMissionRef);
    }

    @Test
    // TODO
    public void testMapEntityUpdate () throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var auxDataRef = getAuxDataRef();
        final var output = ((EndTask) auxDataRef.getTrace().getTask()).getOutput();

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
        ((EndTask) auxDataRef.getTrace().getTask()).setOutput(outputUpdate);
        auxDataRef.getTrace().setCustom(customUpdate);
        sink.accept(toMessage(auxDataRef));

        // Then
        output.put("test_string", outputUpdate.get("test_string"));
        final var collection = new HashSet((Collection)output.get("test_strings"));
        collection.add("string2");
        collection.add("string4");
        output.put("test_strings", collection);
        ((Map)(output.get("test_object"))).put("object1", "value10");
        ((Map)(output.get("test_object"))).put("object2", "value2");
        ((Map)(output.get("test_object"))).put("object4", "value4");
        final var custom = new HashMap<>();
        custom.put("destination", output);
        custom.putAll(customUpdate);

        assertThat(entityIngestor.findAll(AuxData.class))
                .hasSize(1)
                .element(0)
                .matches(t -> {
                    System.out.println(t);
                    return true;
                })
                .extracting("custom")
                .isEqualTo(custom);
    }

    @Test
    public void testMissingRule () throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var fTrace = new FilteredTrace("test", new Trace());

        // When
        final var assertThrown = assertThatThrownBy(() -> sink.accept(toMessage(fTrace)));

        // Then
        assertThrown.isNotNull()
                .hasMessage("No configuration found for 'test'");
    }

    @Test
    public void testFindEntityByField () throws Exception {
        // Given
        final var sink = conf.traceIngestor(factory, entityIngestor);
        final var dsibRef = getDsibRef();
        final var filename = ((EndTask)dsibRef.getTrace().getTask()).getOutput().get("filename").toString();

        // When
        sink.accept(toMessage(dsibRef));
        sink.accept(toMessage(dsibRef));

        // Then
        System.out.println("############################");
        System.out.println(entityIngestor.findEntityBy(Map.of("filename", filename)));
    }


    // -- Helper -- //

    private FilteredTrace getDsibRef() throws  JsonProcessingException {
        var ref = getRawRef();
        ref.getTask().setName("DSIB Trace");
        ((EndTask)ref.getTask()).getOutput().put("filename", "DCS_05_S2B_20210927072424023813_ch1_DSIB.xml");

        return new FilteredTrace("dsib", ref);
    }

    private FilteredTrace getChunkRef() throws  JsonProcessingException {
        var ref = getRawRef();
        ref.getTask().setName("CHUNK Trace");
        ((EndTask)ref.getTask()).getOutput().put("filename", "DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw");

        return new FilteredTrace("chunk", ref);
    }

    private FilteredTrace getMultiChunkRef(String ... additionalChunks) throws  JsonProcessingException {
        var ref = getRawRef();
        ref.getTask().setName("CHUNK Trace");
        final var filenames = new Vector<String>();
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00002.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00004.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00005.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00006.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00007.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00008.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00009.raw");
        filenames.add("DCS_05_S2B_20210927072424023813_ch1_DSDB_00010.raw");
        Arrays.stream(additionalChunks)
                .forEach(ac -> filenames.add(ac));
        ((EndTask)ref.getTask()).getOutput().put("filename", filenames);

        return new FilteredTrace("chunk", ref);
    }

    private FilteredTrace getAuxDataRef() throws  JsonProcessingException {
        var ref = getRawRef();
        ref.getTask().setName("AUX_DATA Trace");
        ((EndTask)ref.getTask()).getOutput().put("filename", "S2B_OPER_GIP_R2EQOG_MPC__20220315T151100_V20220317T000000_21000101T000000_B10");

        return new FilteredTrace("aux_data", ref);
    }

    private Trace getRawRef () throws JsonProcessingException {
        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setMission("S2");

        final var task = new EndTask();
        task.setSatellite("S2B");

        final var mapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        final var output = new HashMap<>();
        output.put("pickup_point_seen_date", "2019-01-21T05:24:40.000000Z");
        output.put("pickup_point_available_date", "2019-01-21T05:24:40.000000Z");
        output.put("ingestion_date", "2019-01-21T05:24:40.000000Z");
        output.put("catalog_storage_date", "2019-01-21T05:24:40.000000Z");
        output.put("channel_id", "ch_1");
        output.put("station", "XBIP");
        output.put("test_strings", new Vector(List.of("string1", "string3")));
        output.put("test_string", "string10");
        output.put("test_object", new HashMap(Map.of("object1", "value1", "object3", "value3")));
        task.setOutput(output);

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setTask(task);

        final var custom = new HashMap<>();
        custom.put("key1", "value1");
        custom.put("key2", "value2");
        custom.put("key3", "value3");
        trace.setCustom(custom);

        return trace;
    }

    private Message<FilteredTrace> toMessage(FilteredTrace ref) {
        return new GenericMessage<>(ref);
    }
}
