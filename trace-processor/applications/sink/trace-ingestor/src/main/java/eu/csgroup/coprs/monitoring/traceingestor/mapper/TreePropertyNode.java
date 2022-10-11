package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode()
public class TreePropertyNode implements TreeProperty {

    private final List<String> paths = new ArrayList<>();
    private final List<TreePropertyLeaf> leafs = new ArrayList<>();

    private final List<TreePropertyNode> nodes = new ArrayList<>();

    public TreePropertyNode() {
        super();
    }

    public void addLeaf(TreePropertyLeaf leaf) {
        add(leafs, leaf);
    }

    public void addNode(TreePropertyNode node) {
        add(nodes, node);
    }

    public void addAllNode (List<TreePropertyNode> nodes) {
        this.nodes.addAll(nodes);
    }

    public void addPath(String path) {
        paths.add(path);
    }

    public void addAllPath(List<String> paths) {
        this.paths.addAll(paths);
    }

    private <T extends TreeProperty> void add(List<T> list, T treeProperty) {
        list.add(treeProperty);
    }

    public TreePropertyNode copy() {
        final var newNode = new TreePropertyNode();

        newNode.addAllPath(this.getPaths());

        for (var node: this.getNodes()) {
            newNode.addNode(node.copy());
        }

        for (var leaf: this.getLeafs()) {
            newNode.addLeaf(leaf.copy());
        }

        return newNode;
    }
}
