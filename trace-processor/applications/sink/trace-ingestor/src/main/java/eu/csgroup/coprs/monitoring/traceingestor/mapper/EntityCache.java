package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Store {@link DefaultEntity} and {@link BeanProperty} in a tree structure form (see {@link TreePropertyNode})<br>
 * <br>
 * Entities and bean property values are associated to a node. Each node is linked to a parent node which is used to propagate
 * bean property values on intermediate node to end nodes.<br>
 * <br>
 * <br>
 * Concept of this class is to store bean property values little by little as node are parsed. When the last node of a
 * branch is reached, call function to retrieve entities on active node to initialize them by calling handler with
 * accumulated bean property values. You can then populate initial entities with accumulated bean property values.
 * <br>
 * <br>
 * To store bean property value and/or entities on a specific node call {@link #setActiveNode(TreePropertyNode)} function
 * and then {@link #setActiveEntities(List)} function and/or {@link #setPropertyValue(Mapping, Object)}.<br>
 * <br>
 * To retrieve entities and/or bean property values for a specific values, use the same function as described above and
 * then call {@link #getActiveEntities()} function and/or {@link #getPropertiesForActiveNode()}<br>
 * An attempt to retrieve entities on a node which is not initialized, cache will retrieve entities in {@link DefaultHandler}
 * by using stored bean property values.
 * <br>
 * To retrieve entities on all node use {@link #dump()} function.
 */
@Slf4j
@RequiredArgsConstructor
public class EntityCache {

    private final DefaultHandler handler;

    private final Map<TreePropertyNode, List<EntityProcessing>> entities = new HashMap<>();

    private final Map<TreePropertyNode, Map<Mapping, Object>> treeProperties = new HashMap<>();

    private final Map<TreePropertyNode, TreePropertyNode> parentsNode = new HashMap<>();


    private TreePropertyNode activeNode;

    /**
     * Set active node and initialize bean property value with those of the parent node.
     *
     * @param node New active node
     */
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
                .ifPresent(nodeProperties::putAll);

        // Set relation between parent and child
        node.getNodes().forEach(subNode -> parentsNode.put(subNode, node));
    }

    /**
     *
     * @return bean property values for the active node.
     */
    public Map<Mapping, Object> getPropertiesForActiveNode () {
        return treeProperties.get(activeNode);
    }

    /**
     * Remove entities for the active node
     */
    public void discardActiveEntities () {
        Optional.ofNullable(entities.get(activeNode))
                .ifPresent(List::clear);
    }

    /**
     *
     * @return entities for the active node
     */
    public List<EntityProcessing> getActiveEntities () {
        return entities.computeIfAbsent(activeNode, k -> handler.getDefaultEntity(treeProperties.get(activeNode)));
    }

    /**
     * Set entities for the active node
     *
     * @param entities new entities to set
     */
    public void setActiveEntities (List<EntityProcessing> entities) {
        this.entities.put(activeNode, entities);
    }

    /**
     * Dump but does not remove entities of all node
     *
     * @return all stored entities
     */
    public List<EntityProcessing> dump () {
        return entities.values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * Set bean property value for the active node
     *
     * @param mapping mapping of the value
     * @param value the value
     */
    public void setPropertyValue (Mapping mapping, Object value) {
        treeProperties.computeIfAbsent(activeNode, k -> new HashMap<>())
                .put(mapping, value);
    }

    public EntityProcessing copy (EntityProcessing entity) {
        return handler.clone(entity.getEntity());
    }
}
