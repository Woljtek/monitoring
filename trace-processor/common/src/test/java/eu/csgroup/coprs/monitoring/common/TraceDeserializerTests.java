package eu.csgroup.coprs.monitoring.common;

import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.csgroup.coprs.monitoring.common.datamodel.*;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TraceDeserializerTests {
    @Test
    public void testNominalBegin() throws IOException {
        // Given
        final var mapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        final var expected = getExpectedBegin(mapper);

        // When
        final var trace = loadTrace("trace-begin.json");

        // Then
        assertThat(trace).isEqualTo(expected);
    }

    @Test
    public void testNominalEnd() throws IOException {
        // Given
        final var mapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        final var expected = getExpectedEnd(mapper);

        // When
        final var trace = loadTrace("trace-end.json");

        // Then
        assertThat(trace).isEqualTo(expected);
    }

    @Test
    public void testMissingRequired() throws IOException {
        // Given

        // When

        // Then
        assertThatThrownBy(() -> loadTrace("trace-MissingRequired.json")).isNotNull();
    }

    @Test
    public void testInvalidUid() throws IOException {
        // Given
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        final var trace = loadTrace("trace-InvalidUid.json");

        // When
        Set<ConstraintViolation<Trace>> violations = validator.validate(trace);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void testInvalidTimestamp() throws IOException {
        // Given

        // When

        // Then
        assertThatThrownBy(() -> loadTrace("trace-InvalidTimestamp.json")).isNotNull();
    }

    @Test
    public void testInvalidContent() throws IOException {
        // Given
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        final var trace = loadTrace("trace-InvalidContent.json");

        // When
        Set<ConstraintViolation<Trace>> violations = validator.validate(trace);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void testEquality () throws IOException {
        // Given
        final var mapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        final var begin1 = getExpectedBegin(mapper);
        final var begin2 = getExpectedBegin(mapper);
        final var end1 = getExpectedEnd(mapper);
        final var end2 = getExpectedEnd(mapper);

        // When
        ((BeginTask)begin2.getTask()).setChildOfTask("toto");
        ((EndTask)end2.getTask()).setErrorCode(404);

        // Then
        assertThat(begin1).isNotEqualTo(begin2);
        assertThat(end1).isNotEqualTo(end2);
    }


    // -- Helper -- //

    protected Trace loadTrace(String classpathResource, JsonMapper mapper) throws IOException{
        final var content = getContent(classpathResource);

        return mapper.readValue(content, Trace.class);
    }

    protected Trace loadTrace(String classpathResource) throws IOException{
        final var mapper = JsonMapper.builder()
                .findAndAddModules()
                .build();

        return loadTrace(classpathResource, mapper);
    }

    protected String getContent(String classpathResource) {
        try {
            return IOUtils.resourceToString(classpathResource, StandardCharsets.UTF_8, TraceDeserializerTests.class.getClassLoader());
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
