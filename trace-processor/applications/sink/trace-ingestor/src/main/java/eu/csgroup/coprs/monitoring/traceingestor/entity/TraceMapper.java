package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.NullValueInNestedPathException;

import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
public class TraceMapper<T extends ExternalInput> {

    public List<T> map(BeanAccessor wrapper, List<Mapping> mappings, DefaultHandler handler) {
        if (mappings != null && mappings.size() != 0) {
            return new Parser(mappings.get(0).entityPath().getBeanName(), mappings).parse(wrapper, handler);
        } else {
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
            if (cache) {
                cachedProperties.put(property, value);
            }
        }

        public boolean hasNext() {
            return current != null ? current.hasNext() : false;
        }

        public void nextEntity () {
            if (current != null && ! current.isPreFilled()) {
                current = handler.clone(current.getEntity());
            } else {
                current = handler.getNextEntity();
                cachedProperties.entrySet()
                        .stream()
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
                        .forEach(ed -> tempCache.add(ed));
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
        private final String entityName;

        private final List<Mapping> rules;

        public List<T> parse(BeanAccessor wrapper, DefaultHandler handler) {
            final var entityCache = new EntityCache(handler);
            parse(rules.iterator(), wrapper, entityCache);

            return entityCache.cached
                    .stream()
                    .map(EntityDescriptor::getEntity)
                    .collect(Collectors.toList());
        }

        public void parse(Iterator<Mapping> iterator, BeanAccessor wrapper, EntityCache entityCache) {
            List<T> entities = new Vector<>();
            while (iterator.hasNext()) {
                var rule = iterator.next();

                try {
                    var value = wrapper.getPropertyValue(rule.tracePath());

                    // Do not set null property to avoid non handled null value conversion
                    if (value != null) {
                        try {
                            entityCache.setPropertyValue(rule.entityPath(), value);
                        } catch (ConversionNotSupportedException e) {
                            // Attempt to convert multi value property to single value property
                            if (value instanceof Collection<?>) {
                                // Continue to parse non multi value properties
                                parse(iterator, wrapper, entityCache);

                                // Then handle multi value property
                                handlePropertyWithMultiValue(rule, ((Iterable) value).iterator(), entityCache);
                            } else {
                                throw e;
                            }
                        }
                    } else {
                        log.warn("No value found for '%s'".formatted(rule.tracePath()));
                    }
                } catch (NullValueInNestedPathException e) {
                    log.warn(e.getMessage());
                }
            }

            entityCache.flush();
        }

        public void handlePropertyWithMultiValue(Mapping rule, Iterator multiValueProperty, EntityCache entityCache) {
            try {
                entityCache.setPropertyValue(rule.entityPath(), multiValueProperty.next(), false);

                entityCache.duplicate(rule.entityPath(), multiValueProperty);
            } catch (NoSuchElementException e) {
                // Handled
            }
        }
    }
}
