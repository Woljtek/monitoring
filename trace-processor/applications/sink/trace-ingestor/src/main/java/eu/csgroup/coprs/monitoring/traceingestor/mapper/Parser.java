package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.InvalidPropertyException;

import java.util.*;

@Slf4j
public record Parser(List<Mapping> rules) {
    public TreePropertyNode parse(BeanAccessor wrapper) {
        return parse(rules.iterator(), wrapper);
    }

    public TreePropertyNode parse(Iterator<Mapping> iterator, BeanAccessor wrapper) {
        final var tree = new TreePropertyNode("");
        while (iterator.hasNext()) {
            parse(tree, iterator.next(), wrapper);
        }

        return tree;
    }

    private void createOrUpdateLeaf (TreePropertyNode tree, Mapping rule, BeanProperty beanProperty, Object value) {
        final var existingLeaf = tree.getLeafs()
                .stream()
                .filter(leaf -> leaf.getRule().equals(rule))
                .findFirst();

        if (existingLeaf.isPresent()) {
            existingLeaf.get().putRawValue(beanProperty, value);
        } else {
            var leaf = new TreePropertyLeaf(rule);
            leaf.putRawValue(beanProperty, value);

            tree.addLeaf(leaf);
        }
    }

    private void parse(TreePropertyNode tree, Mapping rule, BeanAccessor wrapper) {
        for (var wrappedBeanProperty : rule.getFrom()) {
            final var beanProperty = wrappedBeanProperty.getWrappedObject();
            Object value;
            try {
                value = wrapper.getPropertyValue(beanProperty);

                createOrUpdateLeaf(tree, rule, beanProperty, value);
            } catch (InvalidPropertyException e) {
                // Split bean property path to find array value to create branch (one value in the array per branch)
                final var splittedPath = PropertyUtil.splitPath(beanProperty.getBeanPropertyPath(true));
                // Do not keep bean name.
                final var splittedPathWithoutBeanName = Arrays.copyOfRange(splittedPath, 1, splittedPath.length);
                parsePropertyNode(tree, wrapper, rule, beanProperty, splittedPath[0], splittedPathWithoutBeanName);
            }
        }
    }


    /**
     * Parse node where root path doesn't symbolize an array
     *
     * @param tree Parent node
     * @param wrapper Wrapper encapsulating bean in which we have to retrieve values
     * @param rule mapping rule to which bean property depend on
     *             (useful when mapping contains multiple 'from' and we have to set them in the same leaf)
     * @param beanProperty Current bean property to handle
     * @param rootPath current path of the bean property path (which is a portion of the last one)
     * @param splittedPropertyPath Array containing portion of the path (of bean property) that is not yet parsed
     */
    private void parsePropertyNode(TreePropertyNode tree, BeanAccessor wrapper, Mapping rule, BeanProperty beanProperty,  String rootPath, String[] splittedPropertyPath) {
        var currentPath = rootPath;
        var stopParsing = false;
        var index = 0;

        while (! stopParsing && index < splittedPropertyPath.length) {
            currentPath = PropertyUtil.getPath(currentPath, splittedPropertyPath[index]);

            try {
                final var object = wrapper.getPropertyValue(new BeanProperty(currentPath));

                // End of the path reached; create leaf
                if (!currentPath.isEmpty() && index == splittedPropertyPath.length - 1) {
                    createOrUpdateLeaf(tree, rule, beanProperty, object);
                } else if (object instanceof final Collection<?> collection) {
                    final var remainingSplittedPropertyPath = Arrays.copyOfRange(splittedPropertyPath, index + 1, splittedPropertyPath.length);

                    parsePropertyNode(tree, wrapper, rule, beanProperty, currentPath, remainingSplittedPropertyPath, collection);

                    stopParsing = true;
                } else if (object == null) {
                    createOrUpdateLeaf(tree, rule, beanProperty, null);
                }
            } catch (InvalidPropertyException e) {
                if (! currentPath.matches(".*\\[[a-z0-9_]+\\].*")) {
                    // Refer to a field that is not part of trace structure
                    throw new InterruptedOperationException(
                            "Path %s (%s) refer to a field that is not part of trace structure".formatted(currentPath, beanProperty)
                            , e
                    );
                } else {
                    // Exclude property where path does not match with bean tree
                    log.warn(e.getMessage());

                    stopParsing = true;
                }
            }

            index++;
        }
    }

    /**
     * Parse node where current root path symbolize an array
     *
     * @param tree Parent node
     * @param wrapper Wrapper encapsulating bean in which we have to retrieve values
     * @param rule mapping rule to which bean property depend on
     *             (useful when mapping contains multiple 'from' and we have to set them in the same leaf)
     * @param beanProperty Current bean property to handle
     * @param rootPath current path of the bean property path (which is a portion of the last one)
     * @param splittedPropertyPath Array containing portion of the path (of bean property) that is not yet parsed
     * @param node values of the root path
     */
    private void parsePropertyNode(TreePropertyNode tree, BeanAccessor wrapper, Mapping rule, BeanProperty beanProperty, String rootPath, String[] splittedPropertyPath, Collection<?> node) {
        final var newNodes = new ArrayList<TreePropertyNode>();

        final var treeEmptyOnStart = tree.getNodes().isEmpty();
        var duplicateExistingNode = true;

        for (int index = 0; index < node.size(); index++) {
            final var propertyIndex = "[%s]".formatted(index);
            final var currentPath = PropertyUtil.getPath(rootPath, propertyIndex);

            // Find a node which contains desired path
            var opTreeNode = tree.getNodes()
                    .stream()
                    .filter(subNode -> subNode.getPath().equals(currentPath))
                    .findFirst();


            if (opTreeNode.isEmpty() && ! treeEmptyOnStart) {
                if (index == node.size() - 1) {
                    duplicateExistingNode = false;
                }

                // Spread path on existing node (and duplicate them when needed)
                // Intended to make all possible combination with existing node
                // by updating existing and create missing one.
                newNodes.addAll(
                        spreadPathThenParseNode(
                                tree,
                                duplicateExistingNode,
                                wrapper,
                                rule,
                                beanProperty,
                                currentPath,
                                splittedPropertyPath
                        )
                );
            } else {
                TreePropertyNode treeNode;
                if (opTreeNode.isEmpty()) {
                    // Otherwise create new one
                    treeNode = new TreePropertyNode(currentPath);
                    tree.addNode(treeNode);
                } else {
                    treeNode = opTreeNode.get();
                }

                parsePropertyNode(treeNode, wrapper, rule, beanProperty, currentPath, splittedPropertyPath);
            }
        }

        if (node.isEmpty()) {
            createOrUpdateLeaf(tree, rule, beanProperty, null);
        } else {
            tree.addAllNode(newNodes);
        }
    }

    /**
     * Set path on given node by updating reference or duplicating them before
     * according to duplicate parameter value.
     *
     * @param tree contains node to update or duplicate
     * @param duplicate Set to true to duplicate node before setting path otherwise false.
     * @param wrapper Wrapper encapsulating bean in which we have to retrieve values
     * @param rule mapping rule to which bean property depend on
     *             (useful when mapping contains multiple 'from' and we have to set them in the same leaf)
     * @param beanProperty Current bean property to handle
     * @param rootPath current path of the bean property path (which is a portion of the last one)
     * @param splittedPropertyPath Array containing portion of the path (of bean property) that is not yet parsed
     * @return only duplicated node.
     */
    private List<TreePropertyNode> spreadPathThenParseNode (TreePropertyNode tree, boolean duplicate, BeanAccessor wrapper, Mapping rule, BeanProperty beanProperty, String rootPath, String[] splittedPropertyPath) {
        final var newNodes = new ArrayList<TreePropertyNode>();

        for (var treeNode : tree.getNodes()) {
            TreePropertyNode currentNode;
            if (duplicate) {
                currentNode = treeNode.copy(rootPath);
                // Don't add node directly to the tree otherwise it will be used for next index.
                newNodes.add(currentNode);
            } else {
                currentNode = treeNode;
            }

            parsePropertyNode(currentNode, wrapper, rule, beanProperty, rootPath, splittedPropertyPath);
        }

        return newNodes;
    }
}
