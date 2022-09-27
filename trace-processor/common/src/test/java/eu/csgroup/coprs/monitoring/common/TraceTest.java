package eu.csgroup.coprs.monitoring.common;


import eu.csgroup.coprs.monitoring.common.datamodel.*;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceTest {

    @Test
    public void testEquality () {
        // Given
        final var trace1 = getTrace1();

        // When
        var updatedTrace1 = getTrace1();
        updatedTrace1.getTrace().getTask().setUid("Uid1");
        var updatedTrace2 = getTrace1();
        updatedTrace2.getTrace().getTask().setName("Name2");
        var updatedTrace3 = getTrace1();
        updatedTrace3.getTrace().getTask().setEvent(Event.END);
        var updatedTrace4 = getTrace1();
        updatedTrace4.getTrace().getTask().setDataRateMebibytesSec(20);
        var updatedTrace5 = getTrace1();
        updatedTrace5.getTrace().getTask().setDataVolumeMebibytes(30);
        var updatedTrace6 = getTrace1();
        updatedTrace6.getTrace().getTask().setSatellite("S2A");
        var updatedTrace7 = getTrace1();
        updatedTrace7.getTrace().getTask().setInput(Map.of("Key2", "Value2"));
        var updatedTrace8 = getTrace1();
        ((BeginTask)(updatedTrace8.getTrace().getTask())).setChildOfTask("Child of task 8");
        var updatedTrace9 = getTrace1();
        ((BeginTask)(updatedTrace9.getTrace().getTask())).setFollowsFromTask("Follow from task 9");

        var updatedTrace10 = getTrace1();
        updatedTrace10.getTrace().getHeader().setType(null);
        var updatedTrace11 = getTrace1();
        updatedTrace11.getTrace().getHeader().setTimestamp(Instant.now());
        var updatedTrace12 = getTrace1();
        updatedTrace12.getTrace().getHeader().setLevel(Level.DEBUG);
        var updatedTrace13 = getTrace1();
        updatedTrace13.getTrace().getHeader().setMission("S1");
        var updatedTrace14 = getTrace1();
        updatedTrace14.getTrace().getHeader().setRsChainName("Rs chain name 14");
        var updatedTrace15 = getTrace1();
        updatedTrace15.getTrace().getHeader().setRsChainVersion("Rs chain version 15");
        var updatedTrace16 = getTrace1();
        updatedTrace16.getTrace().getHeader().setWorkflow(Workflow.EXTERNAL_CUSTOM_DEMAND);
        var updatedTrace17 = getTrace1();
        updatedTrace17.getTrace().getHeader().setDebugMode(true);
        var updatedTrace18 = getTrace1();
        updatedTrace18.getTrace().getHeader().setTagList(List.of("Value2"));

        var updatedTrace19 = getTrace1();
        updatedTrace19.getTrace().getMessage().setContent("Content 19");

        var updatedTrace20 = getTrace1();
        updatedTrace20.getTrace().setCustom(Map.of("Key2", "Value2"));
        var updatedTrace21 = getTrace1();
        updatedTrace21.setTimestamp(5678);
        var updatedTrace22 = getTrace1();
        updatedTrace22.setTime("2022-10-11T10:15:30");
        var updatedTrace23 = getTrace1();
        updatedTrace23.setStream("Stream 23");
        var updatedTrace24 = getTrace1();
        updatedTrace24.setP("O");
        var updatedTrace25 = getTrace1();
        updatedTrace25.setKubernetes(Map.of("Key2", "Value2"));

        // Then
        assertThat(trace1).isNotEqualTo(updatedTrace1)
                .isNotEqualTo(updatedTrace2)
                .isNotEqualTo(updatedTrace3)
                .isNotEqualTo(updatedTrace4)
                .isNotEqualTo(updatedTrace5)
                .isNotEqualTo(updatedTrace6)
                .isNotEqualTo(updatedTrace7)
                .isNotEqualTo(updatedTrace8)
                .isNotEqualTo(updatedTrace9)
                .isNotEqualTo(updatedTrace10)
                .isNotEqualTo(updatedTrace11)
                .isNotEqualTo(updatedTrace12)
                .isNotEqualTo(updatedTrace13)
                .isNotEqualTo(updatedTrace14)
                .isNotEqualTo(updatedTrace15)
                .isNotEqualTo(updatedTrace16)
                .isNotEqualTo(updatedTrace17)
                .isNotEqualTo(updatedTrace18)
                .isNotEqualTo(updatedTrace19)
                .isNotEqualTo(updatedTrace20)
                .isNotEqualTo(updatedTrace21)
                .isNotEqualTo(updatedTrace22)
                .isNotEqualTo(updatedTrace23)
                .isNotEqualTo(getTrace2())
                .isEqualTo(getTrace1());

        assertThat(trace1.toString())
                .hasToString(getTrace1().toString())
                .isNotEqualTo(getTrace2().toString());
    }

    private TraceLog getTrace1() {
        final var beginTask = new BeginTask("Child of task", "Follow from task");
        beginTask.setUid("Uid");
        beginTask.setName("Name");
        beginTask.setEvent(Event.BEGIN);
        beginTask.setDataRateMebibytesSec(10);
        beginTask.setDataVolumeMebibytes(20);
        beginTask.setSatellite("S2B");
        beginTask.setInput(Map.of("Key1", "Value1"));

        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setTimestamp(Instant.parse("2022-10-14T10:20:59.00Z"));
        header.setLevel(Level.INFO);
        header.setMission("S2");
        header.setRsChainName("Rs chain name");
        header.setRsChainVersion("Rs chain version");
        header.setWorkflow(Workflow.NOMINAL);
        header.setDebugMode(false);
        header.setTagList(List.of("Value1"));

        final var message = new Message();
        message.setContent("Content");

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setMessage(message);
        trace.setTask(beginTask);
        trace.setCustom(Map.of("Key1", "Value1"));

        final var traceLog = new TraceLog();
        traceLog.setTimestamp(1234);
        traceLog.setTime("2022-10-10T10:15:30");
        traceLog.setStream("Stream");
        traceLog.setP("P");
        traceLog.setTrace(trace);
        traceLog.setKubernetes(Map.of("Key1", "Value1"));

        return traceLog;
    }

    private TraceLog getTrace2() {
        final var endTask = new EndTask(
                Status.OK,
                60,
                70.0,
                Map.of("Key1", "Value1"),
                Map.of("Key1", "Value1"),
                List.of("Value1")
        );

        endTask.setUid("Uid");
        endTask.setName("Name");
        endTask.setEvent(Event.BEGIN);
        endTask.setDataRateMebibytesSec(10);
        endTask.setDataVolumeMebibytes(20);
        endTask.setSatellite("S2B");
        endTask.setInput(Map.of("Key1", "Value1"));

        final var header = new Header(
                TraceType.REPORT,
                Instant.parse("2022-10-14T10:20:59.00Z"),
                Level.INFO,
                "S2",
                "Rs chain name",
                "Rs chain version",
                Workflow.NOMINAL,
                false,
                List.of("Value1")
        );

        final var message = new Message("Content");

        final var trace = new Trace(
                header,
                message,
                endTask,
                Map.of("Key1", "Value1")
        );

        return new TraceLog(
                1234,
                "2022-10-10T10:15:30",
                "Stream",
                "P",
                trace,
                Map.of("Key1", "Value1")
        );
    }
}
