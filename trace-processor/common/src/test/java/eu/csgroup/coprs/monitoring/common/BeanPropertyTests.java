package eu.csgroup.coprs.monitoring.common;

import eu.csgroup.coprs.monitoring.common.properties.ReloadableYamlPropertySource;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BeanPropertyTests {
    @Test
    public void testExtraction() {
        // Given
        final var beanProp = new ReloadableYamlPropertySource(
                "test",
                BeanPropertyTests.class.getClassLoader().getResource("beanProperties.yaml").getPath());
        final var expectedProperties = new HashMap<String, String>();
        expectedProperties.put("filters[0].name", "S2-AUX_DATA");
        expectedProperties.put("filters[0].rules.header.mission", "S2");
        expectedProperties.put("filters[0].rules.header.type", "REPORT");
        expectedProperties.put("filters[0].rules.task.event", "END");
        expectedProperties.put("filters[0].rules.task.status", "OK");
        expectedProperties.put("filters[0].rules.message.content", "End metadata extraction");
        expectedProperties.put("filters[0].rules.[task.input[filename_strings][0]]", ".*_V.*");

        expectedProperties.put( "filters[1].name", "S2-DSIB");
        expectedProperties.put("filters[1].rules.header.mission", "S2");
        expectedProperties.put("filters[1].rules.header.type", "REPORT");
        expectedProperties.put("filters[1].rules.task.event", "END");
        expectedProperties.put("filters[1].rules.task.status", "OK");
        expectedProperties.put("filters[1].rules.message.content", "End processing of");
        expectedProperties.put("filters[1].rules.[task.output[filename_strings][0]]", ".*_DSIB\\.xml");

        expectedProperties.put("filters[2].name", "S2-CHUNK");
        expectedProperties.put("filters[2].rules.header.mission", "S2");
        expectedProperties.put("filters[2].rules.header.type", "REPORT");
        expectedProperties.put("filters[2].rules.task.event", "END");
        expectedProperties.put("filters[2].rules.task.status", "OK");
        expectedProperties.put("filters[2].rules.message.content", "End processing of");
        expectedProperties.put("filters[2].rules.[task.output[filename_strings][0]]", ".*ch2_DSDB.* |.*\\.raw");

        // When
        final var retrievedProperties = Arrays.stream(beanProp.getPropertyNames()).toList();
        final var retrievedValues = expectedProperties.keySet().stream().map(beanProp::getProperty).toList();


        assertThat(retrievedProperties).containsExactlyInAnyOrderElementsOf(expectedProperties.keySet());
        assertThat(retrievedValues).containsExactlyInAnyOrderElementsOf(expectedProperties.values());
    }
}
