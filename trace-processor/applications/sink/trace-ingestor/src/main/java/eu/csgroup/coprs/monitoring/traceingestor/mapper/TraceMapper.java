package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.traceingestor.config.AliasWrapper;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public record TraceMapper(BeanAccessor wrapper, String configurationName) {
    public List<EntityProcessing> map(List<Mapping> mappings, DefaultHandler handler) {
        final var tree = new Parser(mappings).parse(wrapper);

        final var cache = new EntityCache(handler);

        map(tree, cache);

        return cache.dump();
    }

    private void map(TreePropertyNode tree, EntityCache entityCache) throws InterruptedOperationException {
        entityCache.setActiveNode(tree);

        try {
            for (var leaf : tree.getLeafs()) {
                mapLeafs(leaf, entityCache);
            }

            if (tree.getNodes() == null || tree.getNodes().isEmpty()) {
                // Force calculation of entities to use even if there is no mapping
                entityCache.getActiveEntities();

                // Start creating entity for this branch
                entityCache.getPropertiesForActiveNode()
                        .entrySet()
                        .stream()
                        .collect(Collectors.groupingBy(entry -> entry.getKey().getTo()))
                        .forEach((key, entry) -> populateEntities(entityCache, entry));
            } else {
                mapNodes(tree.getNodes(), entityCache);
            }
        } catch (InterruptedOperationException e) {
            entityCache.discardActiveEntities();
        }
    }

    private boolean isValueNotSet(Mapping mapping, List<EntityProcessing> entities) {
        return entities.stream()
                .findFirst()
                .map(entity -> entity.getPropertyValue(mapping.getTo()))
                .map(Objects::isNull)
                .orElse(true);
    }

    private void populateEntities (EntityCache entityCache, List<Map.Entry<Mapping, Object>> mappings) {
        final var mappingsIt = mappings.iterator();
        var initialEntities = entityCache.getActiveEntities();
        List<EntityProcessing> populatedEntities = new ArrayList<>();

        while (mappingsIt.hasNext()) {
            final var entry = mappingsIt.next();

            // Assume that all active entities are identical in terms of value set or not.
            // If one entity has a value set for this field the others entity have also a value set for the same field.
            if (! entry.getKey().isSetValueOnlyIfNull() || isValueNotSet(entry.getKey(), initialEntities)) {
                try {
                    populatedEntities.addAll(populateEntitiesWithSingleValue(entityCache, entry.getKey(), entry.getValue(), initialEntities, false));
                } catch (ConversionNotSupportedException e) {
                    if (entry.getValue() instanceof final Collection<?> collection) {
                        populatedEntities.addAll(populateEntitiesWithMultiValue(entityCache, entry.getKey(), collection, initialEntities));
                    } else {
                        throw e;
                    }
                }
            } else {
                populatedEntities.addAll(initialEntities);
            }

            initialEntities = populatedEntities;
            populatedEntities = new ArrayList<>();
        }

        entityCache.setActiveEntities(initialEntities);
    }

    private List<EntityProcessing> populateEntitiesWithMultiValue (EntityCache entityCache, Mapping mapping, Collection<?> values, List<EntityProcessing> entities) {
        final var result = new ArrayList<EntityProcessing>();
        boolean duplicate = false;

        if (! values.isEmpty()) {
            for (Object value : values) {
                result.addAll(populateEntitiesWithSingleValue(entityCache, mapping, value, entities, duplicate));

                duplicate = true;
            }
        } else if (mapping.isRemoveEntityIfNull()) {
            throw new InterruptedOperationException("Value for property %s can't be null".formatted(mapping.getTo().getRawPropertyPath()));
        }

        return result;
    }

    private List<EntityProcessing> populateEntitiesWithSingleValue (EntityCache entityCache, Mapping mapping, Object value, List<EntityProcessing> entities, boolean duplicate) {
        final var entitiesIt = entities.iterator();
        final var result = new ArrayList<EntityProcessing>();

        while (entitiesIt.hasNext()) {
            EntityProcessing entity;
            if (duplicate) {
                entity = entityCache.copy(entitiesIt.next());
            } else {
                entity =  entitiesIt.next();
            }

            entity.setPropertyValue(mapping.getTo(), value);
            result.add(entity);
        }
        return result;
    }

    private void mapLeafs (TreePropertyLeaf leaf, EntityCache entityCache) {
        final var value = mapPropertyValue(leaf.getRule(), leaf.getRawValues());

        // Do not set null property value to avoid non handled null value conversion
        if (value != null) {
            entityCache.setPropertyValue(leaf.getRule(), value);
        } else {
            log.warn("No value found for '%s' for configuration '%s'%n%s".formatted(leaf.getRule().getFrom(), configurationName, wrapper.getDelegate().getWrappedInstance()));
        }
    }

    private void mapNodes (List<TreePropertyNode> nodes, EntityCache entityCache) {
        for (TreePropertyNode node : nodes) {
            map(node, entityCache);
        }
    }

    public static Object mapPropertyValue(Mapping rule, Map<BeanProperty, Object> rawValues) {
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
