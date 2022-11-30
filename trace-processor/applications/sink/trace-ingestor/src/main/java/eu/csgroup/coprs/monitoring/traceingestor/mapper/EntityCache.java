package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class EntityCache {

    private final DefaultHandler handler;

    private final Map<TreePropertyNode, List<EntityProcessing>> entities = new HashMap<>();

    private final Map<TreePropertyNode, Map<Mapping, Object>> treeProperties = new HashMap<>();

    private final Map<TreePropertyNode, TreePropertyNode> parentsNode = new HashMap<>();


    private TreePropertyNode activeNode;


    public void setActiveNode (TreePropertyNode node) {
        activeNode = node;

        // Prepare property cache for the current node (if not already done)
        final var nodeProperties = Optional.of(node)
                .map(treeProperties::get)
                .orElseGet(() -> {
                    final var cache = new HashMap<Mapping, Object>();
                    treeProperties.put(node, cache);

                    return cache;
                });

        // By default, we only store properties for later use.
        // Retrieve parent properties node
        // Create a copy and set it as active node
        Optional.ofNullable(parentsNode.get(node))
                .map(treeProperties::get)
                .filter(Objects::nonNull)
                .ifPresent(nodeProperties::putAll);

        // Set relation between parent and child
        node.getNodes().forEach(subNode -> parentsNode.put(subNode, node));
    }

    public Map<Mapping, Object> getPropertiesForActiveNode () {
        return treeProperties.get(activeNode);
    }

    /**
     * Remove active entities from cache
     */
    public void discardActiveEntities () {
        Optional.ofNullable(entities.get(activeNode))
                .ifPresent(List::clear);
    }

    public List<EntityProcessing> getActiveEntities () {
        return entities.computeIfAbsent(activeNode, k -> handler.getDefaultEntity(treeProperties.get(activeNode)));
    }

    public void setActiveEntities (List<EntityProcessing> entities) {
        this.entities.put(activeNode, entities);
    }

    public List<EntityProcessing> dump () {
        return entities.values().stream().flatMap(Collection::stream).toList();
    }

    public void setPropertyValue (Mapping mapping, Object value) {
        treeProperties.computeIfAbsent(activeNode, k -> new HashMap<>())
                .put(mapping, value);
    }

    public EntityProcessing copy (EntityProcessing entity) {
        return handler.clone(entity.getEntity());
    }
}
