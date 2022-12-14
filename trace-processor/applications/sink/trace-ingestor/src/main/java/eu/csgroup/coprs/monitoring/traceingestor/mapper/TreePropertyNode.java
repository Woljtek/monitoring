package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode()
@ToString(onlyExplicitlyIncluded = true )
public class TreePropertyNode implements TreeProperty {

    @ToString.Include
    private final String path;

    private final List<TreePropertyLeaf> leaves = new ArrayList<>();

    private final List<TreePropertyNode> nodes = new ArrayList<>();


    public void addLeaf(TreePropertyLeaf leaf) {
        add(leaves, leaf);
    }

    public void addNode(TreePropertyNode node) {
        add(nodes, node);
    }

    public void addAllNode (List<TreePropertyNode> nodes) {
        this.nodes.addAll(nodes);
    }


    private <T extends TreeProperty> void add(List<T> list, T treeProperty) {
        list.add(treeProperty);
    }

    public TreePropertyNode copy(String path) {
        final var newNode = new TreePropertyNode(path);

        for (var node: this.getNodes()) {
            newNode.addNode(node.copy(node.path));
        }

        for (var leaf: this.getLeaves()) {
            newNode.addLeaf(leaf.copy());
        }

        return newNode;
    }

    public Collection<TreePropertyLeaf> getAllLeaves() {
       this.nodes.forEach(TreePropertyNode::getAllLeaves);
       List<TreePropertyLeaf> leaves = new ArrayList<>();
       leaves.addAll(this.nodes.stream().flatMap(node -> node.getLeafs().stream()).toList());
       return leaves;
    }
}
