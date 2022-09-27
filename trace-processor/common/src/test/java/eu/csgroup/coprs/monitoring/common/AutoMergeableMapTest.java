package eu.csgroup.coprs.monitoring.common;

import eu.csgroup.coprs.monitoring.common.bean.AutoMergeableMap;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class AutoMergeableMapTest {

    @Test
    public void testAdd() {
        // Given
        final var map = new AutoMergeableMap();
        final var baseMap = getBaseMap();

        // When
        map.putAll(baseMap);

        // Then
        assertThat(map).extracting(checkMap -> checkMap.get("key_string")).isEqualTo(baseMap.get("key_string"));
        assertThat(map).extracting(checkMap -> checkMap.get("key_strings_to_string")).matches(checkValue -> ((Collection<?>)checkValue).containsAll((Collection<?>)baseMap.get("key_strings_to_string")));
        assertThat(map).extracting(checkMap -> checkMap.get("key_strings_to_string2")).matches(checkValue -> ((Collection<?>)checkValue).containsAll((Collection<?>)baseMap.get("key_strings_to_string2")));
        assertThat(map).extracting(checkMap -> checkMap.get("key_strings_to_strings")).matches(checkValue -> ((Collection<?>)checkValue).containsAll((Collection<?>)baseMap.get("key_strings_to_strings")));
        assertThat(map).extracting(checkMap -> checkMap.get("key_object_to_object")).isEqualTo(baseMap.get("key_object_to_object"));
    }

    @Test
    public void testUpdateReplace () {
        // Given
        final var baseMap = getBaseMap();
        final var map = new AutoMergeableMap(baseMap);

        // When
        map.put("key_string", "replaced value");
        map.put("key_strings_to_string", "value 3");
        map.put("key_strings_to_string2", "value 3");
        map.put("key_strings_to_strings", List.of("value 30", "value 40"));
        map.put("key_object", "map replaced by object (other than map)");
        map.put("key_object_to_object", Map.of("key 30", "value 30", "key 40", "value 40"));

        // Then

        // Replace value when not array or map
        assertThat(map).extracting(checkMap -> checkMap.get("key_string")).isNotEqualTo(baseMap.get("key_string"));
        assertThat(map).extracting(checkMap -> checkMap.get("key_string")).isEqualTo("replaced value");

        // Append value to existing array
        final var updatedArray = new ArrayList<>();
        updatedArray.addAll((Collection<?>)baseMap.get("key_strings_to_string"));
        updatedArray.add("value 3");
        assertThat(map).extracting(checkMap -> checkMap.get("key_strings_to_string")).matches(checkValue -> ((Collection<?>)checkValue).containsAll(updatedArray));

        // Do not append duplicate value
        assertThat(map).extracting(checkMap -> checkMap.get("key_strings_to_string2")).matches(checkValue -> ((Collection<?>)checkValue).containsAll((Collection<?>) baseMap.get("key_strings_to_string2")));

        // Append collection to existing array (without duplicate)
        final var updatedArray3 = new ArrayList<>();
        updatedArray3.addAll((Collection<?>)baseMap.get("key_strings_to_strings"));
        updatedArray3.add("value 30");
        updatedArray3.add("value 40");
        assertThat(map).extracting(checkMap -> checkMap.get("key_strings_to_strings")).matches(checkValue -> ((Collection<?>)checkValue).containsAll(updatedArray3));

        // Replace map by object (other than map)
        assertThat(map).extracting(checkMap -> checkMap.get("key_object")).isEqualTo("map replaced by object (other than map)");

        // Append value to map
        assertThat(map).extracting(checkMap -> checkMap.get("key_object_to_object")).isEqualTo(Map.of("key 10", "value for key 10", "key 20", "value for key 20", "key 30", "value 30", "key 40", "value 40"));
    }

    @Test
    public void testDeepUpdate() {
        // Given
        final var baseMap = getBaseMap();
        final var map = new AutoMergeableMap();
        map.put("merge key", baseMap);

        // When
        map.put("merge key", Map.of("key_object_to_object", Map.of("key 30", "value for key 30")));

        // Then
        assertThat(map).extracting(checkMap -> (Map<String, Object>)checkMap.get("merge key"))
                .extracting(checkMap -> (Map<String, Object>)checkMap.get("key_object_to_object"))
                .isInstanceOf(AutoMergeableMap.class)
                .extracting(checkMap -> checkMap.values())
                .matches(list -> list.containsAll(List.of("value for key 10", "value for key 20", "value for key 30")));
    }

    private Map<String, Object> getBaseMap () {
        return Map.of(
                "key_string", "simple value",
                "key_strings_to_string", List.of("value 1", "value 2"),
                "key_strings_to_string2", List.of("value 1", "value 2", "value 3"),
                "key_strings_to_strings", List.of("value 10", "value 20", "value 30"),
                "key_object", Map.of("key 1", "value for key 1", "key 2", "value for key 2"),
                "key_object_to_object", Map.of("key 10", "value for key 10", "key 20", "value for key 20")
        );
    }
}
