package eu.csgroup.coprs.monitoring.tracefilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.tracefilter.json.JsonValidator;
import eu.csgroup.coprs.monitoring.tracefilter.rule.FilterGroup;
import eu.csgroup.coprs.monitoring.tracefilter.rule.Rule;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(FilterGroup.class)
@Import(TraceFilterConfiguration.class)
@ContextConfiguration(initializers = TestInitializer.class)
public class TraceFilterTests {

    private static final TraceFilterConfiguration conf = new TraceFilterConfiguration();

    @Autowired
    private JsonValidator jsonValidator;

    @Autowired
    private ReloadableBeanFactory factory;


    @Test
    public void testFiltered () {
        final var processor = conf.traceFilter(jsonValidator, factory);
        // Given
        final var content = getContent("trace-filtered.json");

        // When
        final var trace = processor.apply(toMessage(content));

        // Then
        assertThat(trace).hasSize(1)
                .map(t -> t.getPayload().getRuleName())
                .allMatch(n -> n.equals("S2-CHUNK"));
    }

    @Test
    public void testNotFiltered () {
        // Given
        final var processor = conf.traceFilter(jsonValidator, factory);
        final var content = getContent("trace-notfiltered.json");

        // When
        final var trace = processor.apply(toMessage(content));

        // Then
        assertThat(trace).isEmpty();
    }

    @Test
    public void testPartial () {
        // Given
        final var processor = conf.traceFilter(jsonValidator, factory);
        final var content = getContent("trace-partial.json");

        // When
        final var trace = processor.apply(toMessage(content));

        // Then
        assertThat(trace).isEmpty();
    }

    @Test
    public void testFatal () {
        // Given
        final var processor = conf.traceFilter(jsonValidator, new MockFactory());
        final var content = getContent("trace-filtered.json");

        // When - Then
        assertThatThrownBy(() -> processor.apply(toMessage(content))).isNotNull();
    }

    @Test
    public void testValidRuleOnList() {
        // Given
        final var rule = new Rule("test", ".*_DSIB\\.xml");

        // When
        final var res = rule.test(List.of(
                "DCS_05_S2B_20210927072424023813_ch1_DSIB.xml",
                "DCS_05_S2B_20210927072424023813_ch2_DSIB.xml",
                "DCS_05_S2B_20210927072424023813_ch3_DSIB.xml")
        );

        // Then
        assertThat(res).isTrue();
    }

    @Test
    public void testInvalidRuleOnList() {
        // Given
        final var rule = new Rule("test", ".*_DSIB\\.xml");

        // When
        final var res = rule.test(List.of(
                "DCS_05_S2B_20210927072424023813_ch1_DSIB.xml",
                "DCS_05_S2B_20210927072424023813_ch1_DSDB_00001.raw",
                "DCS_05_S2B_20210927072424023813_ch2_DSIB.xml")
        );

        // Then
        assertThat(res).isFalse();
    }

    protected static String getContent(String classpathResource) {
        try {
            return IOUtils.resourceToString(classpathResource, StandardCharsets.UTF_8, TraceFilterTests.class.getClassLoader());
        } catch (IOException e) {
            throw new RuntimeException("Cannot retrieve content of resource %s".formatted(classpathResource), e);
        }
    }

    @Test
    public void testFilterConf () {

    }

    private Message<String> toMessage(String jsonContent) {
        return new GenericMessage<>(jsonContent);
    }

    private class MockFactory extends ReloadableBeanFactory {
        @Override
        public <T> T getBean(Class<T> className) {
            return null;
        }
    }

}
