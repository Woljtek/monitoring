package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.InvalidPropertyException;

import java.util.*;

@Data
@Slf4j
public class TraceMapper<T extends DefaultEntity> {

    public List<BeanAccessor> map(BeanAccessor wrapper, List<Mapping> mappings, String configurationName, DefaultHandler handler) {
        try {
            return new Parser(mappings, configurationName).parse(wrapper, handler);
        } catch (TraceMapperInterruptedException e) {
            log.warn("", e);
            return List.of();
        }
    }

    @Data
    private class EntityCache {
        private final DefaultHandler<T> handler;

        private final Map<BeanProperty, Object> cachedProperties = new HashMap<>();

        private EntityDescriptor<T> current;

        private List<EntityDescriptor<T>> cached;


        public EntityCache(DefaultHandler<T> handler) {
            this.handler = handler;
            this.cached = new Vector<>();
            nextEntity();
        }

        public void setPropertyValue (BeanProperty property, Object value) {
            setPropertyValue(property, value, true);
        }

        public void setPropertyValue (BeanProperty property, Object value, boolean cache) {
            current.getBean().setPropertyValue(property, value);
            log.debug("Set value %s for property %s".formatted(value, property));
            if (cache) {
                cachedProperties.put(property, value);
            }
        }

        public boolean hasNext() {
            return current != null && current.hasNext();
        }

        public void nextEntity () {
            if (current != null && ! current.isPreFilled()) {
                current = handler.clone(current.getEntity());
            } else {
                current = handler.getNextEntity();
                cachedProperties.entrySet()
                        .forEach(entry -> current.getBean().setPropertyValue(
                                entry.getKey(),
                                entry.getValue()));

            }
            cached.add(current);
        }

        public void duplicate (BeanProperty property, Iterator<Object> valuesIt) {
            final var tempCache = new Vector<>(cached);
            while (valuesIt.hasNext()) {
                final var next = valuesIt.next();
                cached.stream()
                        .map(EntityDescriptor::getEntity)
                        .map(handler::clone)
                        .peek(ed -> ed.getBean().setPropertyValue(property, next))
                        .forEach(tempCache::add);
            }

            cached = tempCache;
        }

        public void flush() {
            while (hasNext()) {
                nextEntity();
            }
        }
    }

    @Data
    private class Parser {

        private final List<Mapping> rules;

        private final String configurationName;
  
        public List<BeanAccessor> parse(BeanAccessor wrapper, DefaultHandler handler) throws TraceMapperInterruptedException {
            final var entityCache = new EntityCache(handler);
            parse(rules.iterator(), wrapper, entityCache);

            return entityCache.cached
                    .stream()
                    .map(EntityDescriptor::getBean)
                    .toList();
        }

        public void parse(Iterator<Mapping> iterator, BeanAccessor wrapper, EntityCache entityCache) throws TraceMapperInterruptedException {
            while (iterator.hasNext()) {
                var rule = iterator.next();
                Object value = null;
                try {
                    value = wrapper.getPropertyValue(rule.getFrom());
                } catch (InvalidPropertyException e) {
                    log.warn(e.getMessage());
                }

                if (value != null) {
                    try {
                        value = ConversionUtil.convert(rule.getMatch(), rule.getConvert(), value);
                        // Do not set null property to avoid non handled null value conversion
                        if (value != null) {
                            entityCache.setPropertyValue(rule.getTo(), value);
                        } else {
                            throw new TraceMapperInterruptedException("Discard mapping of entity %s because value %s does not match or can't be converted %s".formatted(
                                    entityCache.getCurrent().getEntity().getClass().getSimpleName(),
                                    value,
                                    rule
                            ));
                        }
                    } catch (ConversionNotSupportedException | ClassCastException e) {
                        // Attempt to convert multi value property to single value property
                        if (value instanceof Collection<?>) {
                            log.debug("Fill property with multi value");
                            // Continue to parse non multi value properties
                            parse(iterator, wrapper, entityCache);

                            // Then handle multi value property
                            var valIter = ((Iterable) value).iterator();
                            if (rule.getMatch() != null) {
                                valIter = ((Collection) value).stream()
                                        .map(val -> ConversionUtil.convert(rule.getMatch(), rule.getConvert(), (String) val))
                                        .filter(Objects::nonNull)
                                        .iterator();
                            }

                            handlePropertyWithMultiValue(rule, valIter, entityCache);
                        } else {
                            throw e;
                        }
                    }
                } else if (rule.isRemoveEntityIfNull()) {
                    throw new TraceMapperInterruptedException("Value for property %s can't be null".formatted(rule.getTo().getRawPropertyPath()));
                } else {
                    log.warn("No value found for '%s' for configuration '%s'\n%s".formatted(rule.getFrom(), configurationName, wrapper.getDelegate().getWrappedInstance()));
                }

            }

            entityCache.flush();
        }

        public void handlePropertyWithMultiValue(Mapping rule, Iterator multiValueProperty, EntityCache entityCache) {
            try {
                entityCache.setPropertyValue(rule.getTo(), multiValueProperty.next(), false);

                entityCache.duplicate(rule.getTo(), multiValueProperty);
            } catch (NoSuchElementException e) {
                // Handled
            }
        }
    }

    private static class TraceMapperInterruptedException extends Exception {
        public TraceMapperInterruptedException (String message) {
            super(message);
        }
    }
}
