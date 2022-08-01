package eu.csgroup.coprs.monitoring.common.bean;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Auto mergeable map which apply merge functionnality (not based on {@link Map#merge(Object, Object, BiFunction)})
 * automatically when putting a value.
 * Depending on old value and new value type merge result will vary.
 *
 * When merging value(s) into collection or map, a new one is created and the old one is left intact.
 */
public class AutoMergeableMap extends HashMap<Object, Object> {

    public AutoMergeableMap() {
        super();
    }

    public AutoMergeableMap(Map map) {
        super(map);
    }

    @Override
    public Object put(Object key, Object value) {
        var newValue = value;
        Object oldValue = null;
        if (containsKey(key)) {
            oldValue = get(key);

            newValue = merge(oldValue, value);
        } else if (value instanceof Map<?,?>) {
            newValue = convertMap((Map)value);
        }

        if (oldValue != newValue) {
            super.put(key, newValue);
        }
        return oldValue;
    }

    private Collection updateList (Collection oldValue, Object value) {
        final var mergedValue = new HashSet<>(oldValue);

        if (value instanceof Collection<?>) {
            mergedValue.addAll((Collection) value);
        } else {
            mergedValue.add(value);
        }

        return mergedValue;
    }

    private Map updateMap (Map oldValue, Map value) {
        final var mergedValue = new AutoMergeableMap(oldValue);

        value.forEach((k, v) -> mergedValue.merge(k, v, this::merge));
        return mergedValue;
    }

    private Object merge(Object oldValue, Object newValue) {
        if (oldValue instanceof Collection<?>) {
            return updateList((Collection) oldValue, newValue);
        } else if (oldValue instanceof Map<?,?> && newValue instanceof Map<?,?>) {
            return updateMap((Map)oldValue, (Map)newValue);
        } else {
            return newValue;
        }
    }

    private Map convertMap(Map map) {
        var collector = Collectors.<Entry, Object, Object, AutoMergeableMap>toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (o1, o2) -> o1,
                AutoMergeableMap::new);

        return (Map)(map.entrySet()
                .stream()
                .collect(collector));
    }

    private Object convertObject(Object obj) {
        if (obj instanceof Collection<?> && ! (obj instanceof HashSet<?>)) {
            return new HashSet((Collection)obj);
        } else if (obj instanceof Map<?,?> && ! (obj instanceof HashMap<?,?>)) {
            return convertMap((Map) obj);
        } else {
            return obj;
        }
    }
}
