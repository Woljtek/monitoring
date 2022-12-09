package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.datamodel.EndTask;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.Parser;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.TreePropertyNode;
import org.junit.Test;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTests {

    @Test
    public void testArrayLeaf () {
        // Given
        final var mapping = new Mapping();
        mapping.setFrom(List.of("task.input[filename_strings]", "task.output[filename_strings]"));
        final var rules = List.of(mapping);
        final var parser = new Parser(rules);

        final var endTask = new EndTask();
        endTask.setInput(Map.of(
                "filename_strings", List.of("input filename_1", "input filename_2")
        ));
        endTask.setOutput(Map.of(
                "filename_strings", List.of("output filename_1", "output filename_2", "output filename_3", "output filename_4", "output filename_5")
        ));

        // When
        final var res = parser.parse(BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(endTask)));

        // Then
        assertThat(res.getNodes()).isEmpty();
        assertThat(res.getLeaves())
                .hasSize(1);
    }

    @Test
    public void testArrayNode () {
        // Given
        final var mapping = new Mapping();
        mapping.setFrom(List.of("task.input[filename_strings][final]", "task.output[filename_strings][final]"));
        final var rules = List.of(mapping);
        final var parser = new Parser(rules);

        final var endTask = new EndTask();
        endTask.setInput(Map.of(
                "filename_strings", List.of(
                        Map.of("final", "input filename_1"),
                        Map.of("final", "input filename_2")
                ))
        );
        endTask.setOutput(Map.of(
                "filename_strings", List.of(
                        Map.of("final", "output filename_1"),
                        Map.of("final", "output filename_2"),
                        Map.of("final", "output filename_3"),
                        Map.of("final", "output filename_4"),
                        Map.of("final", "output filename_5")
                ))
        );

        final var combination = new ArrayList<List<String>>();
        combination.add(List.of("Task.input[filename_strings][0]", "Task.output[filename_strings][0]"));
        combination.add(List.of("Task.input[filename_strings][0]", "Task.output[filename_strings][1]"));
        combination.add(List.of("Task.input[filename_strings][0]", "Task.output[filename_strings][2]"));
        combination.add(List.of("Task.input[filename_strings][0]", "Task.output[filename_strings][3]"));
        combination.add(List.of("Task.input[filename_strings][0]", "Task.output[filename_strings][4]"));
        combination.add(List.of("Task.input[filename_strings][1]", "Task.output[filename_strings][0]"));
        combination.add(List.of("Task.input[filename_strings][1]", "Task.output[filename_strings][1]"));
        combination.add(List.of("Task.input[filename_strings][1]", "Task.output[filename_strings][2]"));
        combination.add(List.of("Task.input[filename_strings][1]", "Task.output[filename_strings][3]"));
        combination.add(List.of("Task.input[filename_strings][1]", "Task.output[filename_strings][4]"));

        // When
        final var res = parser.parse(BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(endTask)));

        // Then
        assertThat(res.getNodes())
                .hasSize(10)
                .allMatch(node -> {
                            final var finalList = combination.stream()
                                    .filter(list -> list.containsAll(node.getNodes().stream().map(TreePropertyNode::getPath).toList()))
                                    .findFirst();
                            if (finalList.isPresent()) {
                                combination.remove(finalList.get());
                                return true;
                            } else {
                                return false;
                            }
                        }
                );
        assertThat(combination).isEmpty();
        assertThat(res.getLeaves()).isEmpty();
    }
}
