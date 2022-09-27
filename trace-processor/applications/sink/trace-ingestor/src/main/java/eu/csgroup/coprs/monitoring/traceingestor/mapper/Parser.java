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
            var rule = iterator.next();
            Object value;
            try {
                value = wrapper.getPropertyValue(rule.getFrom());

                var propertyPath = rule.getFrom().getBeanPropertyPath(true);

                var leaf = new TreePropertyLeaf(propertyPath, rule, value);
                tree.putLeaf(leaf);
            } catch (InvalidPropertyException e) {
                final var splittedPath = PropertyUtil.splitPath(rule.getFrom().getBeanPropertyPath(true));
                // Do not keep bean name.
                final var splittedPathWithoutBeanName = Arrays.copyOfRange(splittedPath, 1, splittedPath.length);
                parsePropertyNode(tree, wrapper, rule, splittedPath[0], splittedPathWithoutBeanName);
            }
        }

        return tree;
    }


    private void parsePropertyNode(TreePropertyNode tree, BeanAccessor wrapper, Mapping rule, String rootPath, String[] splittedPropertyPath) {
        var currentPath = rootPath;

        for (int index = 0; index < splittedPropertyPath.length; index++) {
            currentPath = PropertyUtil.getPath(currentPath, splittedPropertyPath[index]);

            try {
                final var object = wrapper.getPropertyValue(new BeanProperty(currentPath));

                if (!currentPath.isEmpty() && index == splittedPropertyPath.length - 1) {
                    final var treeProp = new TreePropertyLeaf(currentPath, rule, object);
                    tree.putLeaf(treeProp);
                } else if (object instanceof final Collection<?> collection) {
                    final var remainingSplittedPropertyPath = Arrays.copyOfRange(splittedPropertyPath, index + 1, splittedPropertyPath.length);

                    parsePropertyNode(tree, wrapper, rule, currentPath, remainingSplittedPropertyPath, collection);
                    break;
                } else if (object == null) {
                    final var leaf = new TreePropertyLeaf(rule.getFrom().getBeanPropertyPath(true), rule, null);
                    tree.putLeaf(leaf);
                }
            } catch (InvalidPropertyException e) {
                // Exclude property where path does not match with bean tree
                log.warn(e.getMessage());
            }
        }
    }

    private void parsePropertyNode(TreePropertyNode tree, BeanAccessor wrapper, Mapping rule, String rootPath, String[] splittedPropertyPath, Collection<?> node) {

        for (int index = 0; index < node.size(); index++) {
            final var propertyIndex = "[%s]".formatted(index);
            final var currentPath = PropertyUtil.getPath(rootPath, propertyIndex);

            var treeNode = tree.getNode(currentPath);

            if (treeNode == null) {
                treeNode = new TreePropertyNode(currentPath);
                tree.putNode(treeNode);
            }
            parsePropertyNode(treeNode, wrapper, rule, currentPath, splittedPropertyPath);
        }
    }
}
