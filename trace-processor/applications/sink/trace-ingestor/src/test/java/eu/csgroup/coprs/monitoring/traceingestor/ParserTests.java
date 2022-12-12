package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.datamodel.EndTask;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.Parser;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.TreePropertyLeaf;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.TreePropertyNode;
import org.junit.Test;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
        final var rules = List.of(
                createMapping("task.input[data]"),
                createMapping("task.input[category][sub_category][cat]"),
                createMapping("task.input[category][sub_category][subcat]"),
                createMapping("task.input[category][data]"),
                createMapping("task.output[category][sub_category][cat]"),
                createMapping("task.output[category][sub_category][subcat]"),
                createMapping("task.output[category][data]")
        );
        final var parser = new Parser(rules);

        final var endTask = new EndTask();
        endTask.setInput(Map.of(
                "data", "1989",
                "category", List.of(
                        Map.of("sub_category", List.of(
                                Map.of(
                                        "cat","cat1_input",
                                        "subcat", "subcat1_input"
                                ),
                                Map.of(
                                        "cat","cat1_input",
                                        "subcat", "subcat2_input"
                                )),
                                "data", "10"
                        ),
                        Map.of("sub_category", List.of(
                                Map.of(
                                        "cat","cat2_input",
                                        "subcat", "subcat1_input"
                                ),
                                Map.of(
                                        "cat","cat2_input",
                                        "subcat", "subcat2_input"
                                )),
                                "data", "20"
                        )
                )
        ));
        endTask.setOutput(Map.of(
                "category", List.of(
                        Map.of("sub_category", List.of(
                                Map.of(
                                        "cat","cat1_output",
                                        "subcat", "subcat1_output"
                                ),
                                Map.of(
                                        "cat","cat1_output",
                                        "subcat", "subcat2_output"
                                )),
                                "data", "30"
                        ),
                        Map.of("sub_category", List.of(
                                Map.of(
                                        "cat","cat2_output",
                                        "subcat", "subcat1_output"
                                ),
                                Map.of(
                                        "cat","cat2_output",
                                        "subcat", "subcat2_output"
                                )),
                                "data", "40"
                        )
                )
        ));

        final var firstPathList = List.of(
                "Task.input[category][0]",
                "Task.input[category][1]",
                "Task.output[category][0]",
                "Task.output[category][1]"
        );

        final var allPathList = new ArrayList<String>();
        allPathList.add("Task.input[category][0][sub_category][0]");
        allPathList.add("Task.input[category][0][sub_category][1]");
        allPathList.add("Task.input[category][1][sub_category][0]");
        allPathList.add("Task.input[category][1][sub_category][1]");
        allPathList.add("Task.output[category][0][sub_category][0]");
        allPathList.add("Task.output[category][0][sub_category][1]");
        allPathList.add("Task.output[category][1][sub_category][0]");
        allPathList.add("Task.output[category][1][sub_category][1]");
        allPathList.add("");
        allPathList.addAll(firstPathList);

        // When
        final var res = parser.parse(BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(endTask)));
        final var allNodes = Stream.concat(
                Stream.of(res),
                getAllNodes(res)
        ).toList();

        // Then
        assertThat(res.getNodes())
                .hasSize(4)
                .map(TreePropertyNode::getPath)
                .containsAll(firstPathList);

        assertThat(allNodes)
                .hasSize(13)
                .allMatch(node -> true)
                .map(TreePropertyNode::getPath)
                .containsAll(allPathList);

        assertThat(allNodes)
                .allMatch(this::check);
    }

    private Mapping createMapping (String fromCLause) {
        final var mapping= new Mapping();
        mapping.setFrom(fromCLause);

        return mapping;
    }

    private boolean check (TreePropertyNode node) {
        boolean match = true;
        Optional<TreePropertyLeaf> catLeaf;
        Optional<TreePropertyLeaf> subcatLeaf;

        switch (node.getPath()) {
            case "":
                match = match && node.getLeaves().size() == 1;
                match = match && node.getNodes().size() == 4;
                match = match && node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[data]");
                match = match && node.getLeaves().get(0).getRawValues().get(node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject()).equals("1989");
                break;
            case "Task.input[category][0]":
                match = match && node.getLeaves().size() == 1;
                match = match && node.getNodes().size() == 2;
                match = match && node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][data]");
                match = match && node.getLeaves().get(0).getRawValues().get(node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject()).equals("10");
                match = match && node.getNodes()
                        .stream()
                        .map(TreePropertyNode::getPath)
                        .filter(path -> path.equals("Task.input[category][0][sub_category][0]")
                                || path.equals("Task.input[category][0][sub_category][1]"))
                        .count() == 2;
                break;
            case "Task.input[category][1]":
                match = match && node.getLeaves().size() == 1;
                match = match && node.getNodes().size() == 2;
                match = match && node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][data]");
                match = match && node.getLeaves().get(0).getRawValues().get(node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject()).equals("20");
                match = match && node.getNodes()
                        .stream()
                        .map(TreePropertyNode::getPath)
                        .filter(path -> path.equals("Task.input[category][1][sub_category][0]")
                                || path.equals("Task.input[category][1][sub_category][1]"))
                        .count() == 2;
                break;
            case "Task.output[category][0]":
                match = match && node.getLeaves().size() == 1;
                match = match && node.getNodes().size() == 2;
                match = match && node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][data]");
                match = match && node.getLeaves().get(0).getRawValues().get(node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject()).equals("30");
                match = match && node.getNodes()
                        .stream()
                        .map(TreePropertyNode::getPath)
                        .filter(path -> path.equals("Task.output[category][0][sub_category][0]")
                                || path.equals("Task.output[category][0][sub_category][1]"))
                        .count() == 2;
                break;
            case "Task.output[category][1]":
                match = match && node.getLeaves().size() == 1;
                match = match && node.getNodes().size() == 2;
                match = match && node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][data]");
                match = match && node.getLeaves().get(0).getRawValues().get(node.getLeaves().get(0).getRule().getFrom().get(0).getWrappedObject()).equals("40");
                match = match && node.getNodes()
                        .stream()
                        .map(TreePropertyNode::getPath)
                        .filter(path -> path.equals("Task.output[category][1][sub_category][0]")
                                || path.equals("Task.output[category][1][sub_category][1]"))
                        .count() == 2;
                break;
            case "Task.input[category][0][sub_category][0]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat1_input");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat1_input");
                break;
            case "Task.input[category][0][sub_category][1]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat1_input");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat2_input");
                break;
            case "Task.input[category][1][sub_category][0]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat2_input");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat1_input");
                break;
            case "Task.input[category][1][sub_category][1]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.input[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat2_input");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat2_input");
                break;
            case "Task.output[category][0][sub_category][0]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat1_output");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat1_output");
                break;
            case "Task.output[category][0][sub_category][1]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat1_output");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat2_output");
                break;
            case "Task.output[category][1][sub_category][0]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat2_output");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat1_output");
                break;
            case "Task.output[category][1][sub_category][1]":
                match = match && node.getLeaves().size() == 2;
                match = match && node.getNodes().size() == 0;
                catLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][cat]"))
                        .findFirst();
                subcatLeaf = node.getLeaves()
                        .stream()
                        .filter(leaf -> leaf.getRule().getFrom().get(0).getWrappedObject().getRawPropertyPath().equals("task.output[category][sub_category][subcat]"))
                        .findFirst();

                match = match && catLeaf.isPresent();
                match = match && catLeaf.get().getRawValues().get(catLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("cat2_output");
                match = match && subcatLeaf.isPresent();
                match = match && subcatLeaf.get().getRawValues().get(subcatLeaf.get().getRule().getFrom().get(0).getWrappedObject()).equals("subcat2_output");
                break;
            default:
                match = false;
        }

        return match;
    }

    private Stream<TreePropertyNode> getAllNodes (TreePropertyNode node) {
        return node.getNodes()
                .stream()
                .flatMap(childNode -> Stream.concat(
                        Stream.of(childNode),
                        getAllNodes(childNode)
                ));
    }
}
