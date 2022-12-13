package eu.csgroup.coprs.monitoring.common;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.csgroup.coprs.monitoring.common.datamodel.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TraceLogDeserializerTests {
    @Test
    public void testUnknownFieldSkipped () throws Exception {
        // Given/When
        final var traceLog = loadTraceLog("traceLog-unknownFieldSkipped.json");

        // Then
        assertThat(traceLog).isNotNull();
    }

    @Test
    public void testunknownFieldNotSkipped () throws Exception {
        // Given/When
        var res = assertThatThrownBy(() -> loadTraceLog("traceLog-unknownFieldNotSkipped.json"));

        // Then
        res.isNotNull()
                .isInstanceOf(UnrecognizedPropertyException.class);
    }


    // -- Helper -- //

    protected TraceLog loadTraceLog(String classpathResource, JsonMapper mapper) throws IOException{
        final var content = getContent(classpathResource);

        return mapper.readValue(content, TraceLog.class);
    }

    protected TraceLog loadTraceLog(String classpathResource) throws IOException{
        final var mapper = JsonMapper.builder()
                .findAndAddModules()
                .build();

        return loadTraceLog(classpathResource, mapper);
    }

    protected String getContent(String classpathResource) {
        try {
            return IOUtils.resourceToString(classpathResource, StandardCharsets.UTF_8, TraceLogDeserializerTests.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException("Cannot retrieve content of resource %s".formatted(classpathResource), e);
        }
    }

    protected Trace getExpectedBegin(JsonMapper mapper) throws IOException {
        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setTimestamp(Instant.parse("2021-08-30T15:02:24.125000Z"));
        header.setLevel(Level.INFO);
        header.setMission("S3");
        header.setRsChainName("trace-processor");
        header.setRsChainVersion("1.1.9-rc1");
        header.setWorkflow(Workflow.NOMINAL);
        header.setDebugMode(false);
        header.setTagList(List.of("TAG1", "TAG2", "TAG3"));

        final var message = new Message();
        message.setContent("Start compression processing");
        final var task = new BeginTask();
        task.setUid("4cb9fa49-2c0a-4363-82c3-ea9ab223c53a");
        task.setName("CompressionProcessing");
        task.setEvent(Event.BEGIN);
        task.setDataRateMebibytesSec(783740.123);
        task.setDataVolumeMebibytes(783740.123);
        task.setSatellite("S2B");
        task.setInput(
                Map.of(
                        "key", "value1",
                        "key_string", "value2",
                        "key_strings", List.of("value3", "value4", "value5")
                )
        );
        task.setChildOfTask("a66d3ac2-2483-4891-8151-1bc77e4296e8");
        task.setFollowsFromTask("a66d3ac2-2483-4891-8151-1bc77e4296e8");

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setMessage(message);
        trace.setTask(task);
        trace.setCustom(
                Map.of(
                        "key", "value1",
                        "key_string", "value2",
                        "key_strings", List.of("value3", "value4", "value5")
                )
        );

        return trace;
    }

    protected Trace getExpectedEnd(JsonMapper mapper) throws IOException {
        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setTimestamp(Instant.parse("2021-08-30T15:02:24.125000Z"));
        header.setLevel(Level.INFO);
        header.setMission("S3");
        header.setRsChainName("trace-processor");
        header.setRsChainVersion("1.1.9-rc1");
        header.setWorkflow(Workflow.NOMINAL);
        header.setDebugMode(false);
        header.setTagList(List.of("TAG1", "TAG2", "TAG3"));

        final var message = new Message();
        message.setContent("Start compression processing");
        final var task = new EndTask();
        task.setUid("4cb9fa49-2c0a-4363-82c3-ea9ab223c53a");
        task.setName("CompressionProcessing");
        task.setEvent(Event.END);
        task.setDataRateMebibytesSec(783740.123);
        task.setDataVolumeMebibytes(783740.123);
        task.setSatellite("S2B");
        task.setInput(
                Map.of(
                        "key", "value1",
                        "key_string", "value2",
                        "key_strings", List.of("value3", "value4", "value5")
                )
        );
        task.setStatus(Status.NOK);
        task.setErrorCode(400);
        task.setDurationInSeconds(342.1231);
        task.setOutput(
                Map.of(
                        "key", "value1",
                        "key_string", "value2",
                        "key_strings", List.of("value3", "value4", "value5")
                )
        );
        task.setQuality(
                Map.of(
                        "key", "value1",
                        "key_string", "value2",
                        "key_strings", List.of("value3", "value4", "value5")
                )
        );
        task.setMissingOutput(List.of(
                Map.of(
                        "key", "value1",
                        "key_string", "value2",
                        "key_strings", List.of("value3", "value4", "value5")
                ))
        );

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setMessage(message);
        trace.setTask(task);
        trace.setCustom(
                Map.of(
                        "key", "value1",
                        "key_string", "value2",
                        "key_strings", List.of("value3", "value4", "value5")
                )
        );

        return trace;
    }
}
