package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityDescriptor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@Data
public class EntityCache {
    private final DefaultHandler handler;

    private final Map<BeanProperty, Object> cachedProperties = new HashMap<>();

    private EntityDescriptor current;

    private List<EntityDescriptor> cached;


    public EntityCache(DefaultHandler handler) {
        this.handler = handler;
        this.cached = new ArrayList<>();
        nextEntity();
    }

    public EntityCache(EntityCache cache, Map<BeanProperty, Object> cachedProperties) {
        this.handler = cache.getHandler();
        this.cached = cache.getCached();
        this.current = cache.getCurrent();
        this.cachedProperties.putAll(cache.cachedProperties);
        this.cachedProperties.putAll(cachedProperties);
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
            cachedProperties.forEach((key, value) -> current.getBean().setPropertyValue(
                    key,
                    value));

        }
        cached.add(current);
    }

    public void flush() {
        while (hasNext()) {
            nextEntity();
        }
    }
}
