package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TreePropertyNode extends TreeProperty {

    private Map<String, TreePropertyLeaf> leafs = new HashMap<>();

    private Map<String, TreePropertyNode> nodes = new HashMap<>();

    public TreePropertyNode(String path) {
        super(path);
    }

    public void putLeaf(TreePropertyLeaf leaf) {
        put(leafs, leaf);
    }

    public void putNode(TreePropertyNode node) {
        put(nodes, node);
    }

    public TreePropertyNode getNode(String path) {
        return nodes.get(path);
    }

    private <T extends TreeProperty> void put(Map<String, T> map, T treeProperty) {
        map.put(treeProperty.getPath(), treeProperty);
    }
}
