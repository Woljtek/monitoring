package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.traceingestor.converter.Action;
import eu.csgroup.coprs.monitoring.traceingestor.converter.FormatAction;
import eu.csgroup.coprs.monitoring.traceingestor.converter.MatchAction;
import eu.csgroup.coprs.monitoring.traceingestor.converter.SubtractAction;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionTests {

    @Test
    public void testArgSeparator () {
        // Given
        final var arg1 = "value\\\" 1 with space";
        final var arg2 = "value\\\" 2 with\\\" space";
        final var rawAction = "ACTION \"" + arg1 + "\"" + " \"" + arg2 +  "\"";

        // When
        final var action = new Action(rawAction);

        // Then
        assertThat(action.getAllArgs())
                .hasSize(2)
                .contains(arg1, arg2);
    }

    @Test
    public void testMatch () {
        // Given
        final var rawAction = "MATCH ^.+(?=DSDB)";
        final var arg21 = "DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw";
        final var arg22 = "DCS_05_S2B_20210927072424023813_ch1_DSIB.xml";

        // When
        final var action = new MatchAction(rawAction);
        final var actionMatch = action.execute(List.of(arg21));
        final var actionUnMatch = action.execute(List.of(arg22));

        // Then
        assertThat(actionMatch).isNotNull();
        assertThat(actionUnMatch).isNull();

    }

    @Test
    public void testFormat () {
        // Given
        final var rawAction = "FORMAT ^.+(?=DSDB) \"%1$sDSIB.xml\"";
        final var arg21 = "DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw";
        final var arg22 = "DCS_05_S2B_20210927072424023813_ch1_DSIB.xml";

        // When
        final var action = new FormatAction(rawAction);
        final var formatted = action.execute(List.of(arg21));
        final var unformatted = action.execute(List.of(arg22));

        // Then
        assertThat(formatted)
                .isNotNull()
                .isEqualTo(arg22);
        assertThat(unformatted).isNull();
    }

    @Test
    public void testDateSubstract () {
        // Given
        final var rawAction = "SUBSTRACT refDate date long double int";
        final var refDate = Instant.parse("2022-10-06T14:05:07.00Z");
        final var date = Instant.ofEpochSecond(33);
        // 159s
        final var longDuration = 159L;
        // 89s + 7ms + 18 µs
        final var doubleDuration = 89.007018;
        // 6s
        final var intDuration = 6;

        final var goalDate = refDate.minusNanos(287007018000L);

        // When
        final var action = new SubtractAction(rawAction);
        final var resDate = action.execute(List.of(
                refDate,
                date,
                longDuration,
                doubleDuration,
                intDuration
        ));

        // Then
        assertThat(resDate).isEqualTo(goalDate);
    }

    @Test
    public void testLongSubstract () {
        // Given
        final var rawAction = "SUBSTRACT refLong date long double int";
        final var refLong = 1989L;
        final var date = Instant.ofEpochSecond(33);
        // 159s
        final var longDuration = 159L;
        // 89s + 7ms + 18 µs
        final var doubleDuration = 89.007018;
        // 6s
        final var intDuration = 6;

        final var goalLong = 1989L - 33254;

        // When
        final var action = new SubtractAction(rawAction);
        final var resLong = action.execute(List.of(
                refLong,
                date,
                longDuration,
                doubleDuration,
                intDuration
        ));

        // Then
        assertThat(resLong)
                .isInstanceOf(Long.class)
                .isEqualTo(goalLong);
    }

    @Test
    public void testDoubleSubstract () {
        // Given
        final var rawAction = "SUBSTRACT refDouble date long double int";
        final var refDouble = 1989.4561751;
        final var date = Instant.ofEpochSecond(33);
        // 159s
        final var longDuration = 159L;
        // 89s + 7ms + 18 µs
        final var doubleDuration = 89.007018;
        // 6s
        final var intDuration = 6;

        final var goalDouble = 1989.4561751 - 33254.007018;

        // When
        final var action = new SubtractAction(rawAction);
        final var resDouble = action.execute(List.of(
                refDouble,
                date,
                longDuration,
                doubleDuration,
                intDuration
        ));

        // Then
        assertThat(resDouble)
                .isInstanceOf(Double.class)
                .matches(res -> ((Double)res - goalDouble) <= 0.0000001);
    }

    @Test
    public void testIntSubstract () {
        // Given
        final var rawAction = "SUBSTRACT refInt date long double int";
        final var refInt = 1989;
        final var date = Instant.ofEpochSecond(33);
        // 159s
        final var longDuration = 159L;
        // 89s + 7ms + 18 µs
        final var doubleDuration = 89.007018;
        // 6s
        final var intDuration = 6;

        final var goalInt = 1989 - 33254;

        // When
        final var action = new SubtractAction(rawAction);
        final var resInt = action.execute(List.of(
                refInt,
                date,
                longDuration,
                doubleDuration,
                intDuration
        ));

        // Then
        assertThat(resInt)
                .isInstanceOf(Integer.class)
                .isEqualTo(goalInt);
    }
}
