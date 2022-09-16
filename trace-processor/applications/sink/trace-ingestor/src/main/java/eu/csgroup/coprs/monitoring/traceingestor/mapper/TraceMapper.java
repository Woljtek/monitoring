package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.entity.ConversionUtil;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityDescriptor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;

import java.util.*;

@Data
@Slf4j
public class TraceMapper<T extends DefaultEntity> {
    private final BeanAccessor wrapper;

    private final String configurationName;

    public List<BeanAccessor> map(List<Mapping> mappings, DefaultHandler<T> handler) {
        final var tree = new Parser(mappings/*, configurationName*/).parse(wrapper);

        return map(tree, handler);
    }

    public List<BeanAccessor> map(TreePropertyNode tree, DefaultHandler<T> handler) {
        try {
            var entityCache = new EntityCache<T>(handler);
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

    private void map (/*String rootPath,*/ TreePropertyNode tree, EntityCache entityCache) throws InterruptedOperationException {
        final var propertyCache = new HashMap<BeanProperty, Object>();
        for (TreePropertyLeaf leaf: tree.getLeafs().values()) {
            //final var propertyPath = PropertyUtil.getPath(rootPath, leaf.getPath());
            //final var rawValue = wrapper.getPropertyValue(new BeanProperty(propertyPath));
            final var value = mapPropertyValue(leaf.getRule(), leaf.getRawValue());

            // Do not set null property value to avoid non handled null value conversion
            if (value != null) {
                try {
                    entityCache.setPropertyValue(leaf.getRule().getTo(), value);
                    propertyCache.put(leaf.getRule().getTo(), value);
                } catch (ConversionNotSupportedException e) {
                    if (value instanceof final Collection<?> collection) {
                        if (collection.size() > 0) {
                            final var collectionIt = collection.iterator();

                            while (collectionIt.hasNext()) {
                                final var singleValue = collectionIt.next();

                                // Do not set null property value and so create new entity
                                entityCache.setPropertyValue(leaf.getRule().getTo(), singleValue, false);

                                if (collectionIt.hasNext()) {
                                    entityCache.nextEntity();
                                }
                            }

                        } else if (leaf.getRule().isRemoveEntityIfNull()) {
                            throw new InterruptedOperationException("Value for property %s can't be null".formatted(leaf.getRule().getTo().getRawPropertyPath()));
                        }
                    } else {
                        throw e;
                    }
                }
            } else {
                log.warn("No value found for '%s' for configuration '%s'\n%s".formatted(leaf.getRule().getFrom(), configurationName, wrapper.getDelegate().getWrappedInstance()));
            }
        }

        final var nodeIt = tree.getNodes().values().iterator();
        while (nodeIt.hasNext()) {
            final var node = nodeIt.next();
            map(node, new EntityCache<T>(entityCache, propertyCache));

            if (nodeIt.hasNext()) {
                entityCache.nextEntity();
            }
        }

        /*for (TreePropertyNode node: tree.getNodes().values()) {
            final var propertyPath = PropertyUtil.getPath(rootPath, node.getPath());
            //final var collectionSize = ((Collection<?>)wrapper.getPropertyValue(new BeanProperty(propertyPath))).size();
            for (int index = 0; index < collectionSize; index++) {
                final var propertyPathIndex = "%s[%s]".formatted(propertyPath, index);

                map(propertyPathIndex, node, new EntityCache(entityCache, propertyCache));

                if (index != collectionSize - 1) {
                    entityCache.nextEntity();
                }
            }
        }*/
    }


    public static Object mapPropertyValue(Mapping rule, Object rawValue) throws InterruptedOperationException {
        if (rawValue != null) {
            try {
                var value = ConversionUtil.convert(rule.getMatch(), rule.getConvert(), rawValue);

                if (value == null) {
                    throw new InterruptedOperationException("Discard mapping of entity %s because value %s does not match or can't be converted %s".formatted(
                            rule.getTo().getBeanName(),
                            rawValue,
                            rule
                    ));
                }

                return value;
            } catch (ConversionNotSupportedException | ClassCastException e) {
                if (rawValue instanceof final Collection<?> collection) {
                    return collection.stream()
                            .map(val -> ConversionUtil.convert(rule.getMatch(), rule.getConvert(), (String) val))
                            .filter(Objects::nonNull)
                            .toList();
                } else {
                    throw e;
                }
            }



        } else if (rule.isRemoveEntityIfNull()) {
            throw new InterruptedOperationException("Value for property %s can't be null".formatted(rule.getTo().getRawPropertyPath()));
        }

        return null;
    }
}
