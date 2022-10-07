package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.traceingestor.config.AliasWrapper;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;

import java.util.*;

@Slf4j
public record TraceMapper(BeanAccessor wrapper, String configurationName) {
    public List<BeanAccessor> map(List<Mapping> mappings, DefaultHandler handler) {
        final var tree = new Parser(mappings).parse(wrapper);

        return map(tree, handler);
    }

    public List<BeanAccessor> map(TreePropertyNode tree, DefaultHandler handler) {
        try {
            var entityCache = new EntityCache(handler);
            map(tree, entityCache);

            entityCache.flush();

            return entityCache.getCached()
                    .stream()
                    .map(EntityDescriptor::getBean)
                    .toList();
        } catch (InterruptedOperationException e) {
            log.warn("", e);
            return List.of();
        }
    }

    private void map(TreePropertyNode tree, EntityCache entityCache) throws InterruptedOperationException {
        final var propertyCache = new HashMap<BeanProperty, Object>();

        for (var leaf : tree.getLeafs()) {
            mapLeafs(leaf, entityCache, propertyCache);
        }

        mapNodes(tree.getNodes(), entityCache, propertyCache);
    }

    private void mapLeafs (TreePropertyLeaf leaf, EntityCache entityCache, Map<BeanProperty, Object> propertyCache) {
        if (! leaf.getRule().isSetValueOnlyIfNull() || entityCache.getCurrent().getBean().getPropertyValue(leaf.getRule().getTo()) == null) {
            final var value = mapPropertyValue(leaf.getRule(), leaf.getRawValues());

            // Do not set null property value to avoid non handled null value conversion
            if (value != null) {
                try {
                    entityCache.setPropertyValue(leaf.getRule().getTo(), value);
                    propertyCache.put(leaf.getRule().getTo(), value);
                } catch (ConversionNotSupportedException e) {
                    if (value instanceof final Collection<?> collection) {
                        mapCollection(leaf, entityCache, collection);
                    } else {
                        throw e;
                    }
                }
            } else {
                log.warn("No value found for '%s' for configuration '%s'%n%s".formatted(leaf.getRule().getFrom(), configurationName, wrapper.getDelegate().getWrappedInstance()));
            }
        }
    }

    private void mapNodes (List<TreePropertyNode> nodes, EntityCache entityCache, Map<BeanProperty, Object> propertyCache) {
        final var nodeIt = nodes.iterator();
        while (nodeIt.hasNext()) {
            final var node = nodeIt.next();
            map(node, new EntityCache(entityCache, propertyCache));

            if (nodeIt.hasNext()) {
                entityCache.nextEntity();
            }
        }
    }

    private void mapCollection (TreePropertyLeaf leaf, EntityCache entityCache, Collection<?> collection) {
        if (! collection.isEmpty()) {
            final var collectionIt = collection.iterator();

            while (collectionIt.hasNext()) {
                final var singleValue = collectionIt.next();

                // Do not set null property value and so create new entity
                entityCache.setPropertyValue(leaf.getRule().getTo(), singleValue, false);

                if (collectionIt.hasNext()) {
                    entityCache.nextEntity();
                }
            }

        } else {
            throw new InterruptedOperationException("Value for property %s can't be null".formatted(leaf.getRule().getTo().getRawPropertyPath()));
        }
    }


    public static Object mapPropertyValue(Mapping rule, Map<BeanProperty, Object> rawValues) throws InterruptedOperationException {
        checkInputs(rule, rawValues);

        // Key => alias
        // Value => converted value
        final var availableValues = new ArrayList<AliasWrapper<Object>>();
        rule.getFrom().forEach(from -> availableValues.add(
                new AliasWrapper<>(
                        from.getAlias(),
                        rawValues.get(from.getWrappedObject())
                )
        ));

        final var toExecuteValues = rule.getAction()
                .stream()
                .map(AliasWrapper::getAlias)
                .toList();

        // Assume that conversion action are not in the correct order
        final var unorderedAction = new LinkedList<>(rule.getAction());
        while (! unorderedAction.isEmpty()) {
            final var argValues = new ArrayList<>();
            var currentAction = unorderedAction.removeFirst();
            final var dynamicArgs = currentAction.getWrappedObject().getDynamicArgs();

            int availableIndex = availableValues.size() - 1;
            for (var arg : dynamicArgs) {
                final var matchingAlias = availableValues.stream()
                        .filter(val -> val.getAlias().equals(arg))
                        .findFirst();
                if (matchingAlias.isPresent()) {
                    argValues.add(matchingAlias.get().getWrappedObject());
                } else if (toExecuteValues.contains(arg)){
                    // Not yet available
                    break;
                } else {
                    // Use latest available values
                    argValues.add(availableValues.get(availableIndex--).getWrappedObject());
                }
            }

            if (dynamicArgs.size() != argValues.size()) {
                unorderedAction.add(currentAction);
            } else {
                final var res = currentAction.getWrappedObject().execute(argValues);

                if (res == null) {
                    throw new InterruptedOperationException("Discard mapping of entity %s because value(s) %s does not match or can't be converted %s".formatted(
                            rule.getTo().getBeanName(),
                            argValues,
                            currentAction.getWrappedObject().getRawAction()
                    ));
                }

                availableValues.add(
                        new AliasWrapper<>(
                                currentAction.getAlias(),
                                res
                        )
                );
            }
        }

        return availableValues.get(availableValues.size() - 1).getWrappedObject();

    }

    private static void checkInputs (Mapping rule, Map<BeanProperty, Object> rawValues) {
        for (var rawValue : rawValues.values()) {
            if (rawValue == null && rule.isRemoveEntityIfNull()) {
                throw new InterruptedOperationException("Value for property %s can't be null".formatted(rule.getTo().getRawPropertyPath()));
            }
        }
    }
}
