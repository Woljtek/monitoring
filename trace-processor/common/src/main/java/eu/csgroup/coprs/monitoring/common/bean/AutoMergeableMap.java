package eu.csgroup.coprs.monitoring.common.bean;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Auto mergeable map which apply merge functionnality (not based on {@link Map#merge(Object, Object, BiFunction)})
 * automatically when putting a value.
 * Depending on old value and new value type merge result will vary.
 * <p>
 * When merging value(s) into collection or map, a new one is created and the old one is left intact.
 */
public class AutoMergeableMap extends HashMap<String, Object> {

    public AutoMergeableMap() {
        super();
    }

    public AutoMergeableMap(Map<String, Object> map) {
        super();
        putAll(map);
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
        map.forEach(this::put);
    }

    @Override
    public Object put(String key, Object value) {
        var newValue = value;
        Object oldValue = null;
        if (containsKey(key)) {
            oldValue = get(key);

            newValue = merge(oldValue, value);
        } else if (value instanceof Map<?,?> || value instanceof Collection<?>) {
            newValue = convertObject(value);
        }

        if (oldValue != newValue) {
            super.put(key, newValue);
        }
        return oldValue;
    }

    private Collection<?> updateList (Collection<?> oldValue, Object value) {
        final var mergedValue = new HashSet<Object>(oldValue);

        if (value instanceof Collection<?>) {
            mergedValue.addAll((Collection<?>) value);
        } else {
            mergedValue.add(value);
        }

        return mergedValue;
    }

    private Map<String, Object> updateMap (Map<String, Object> oldValue, Map<String, Object> value) {
        final var mergedValue = new AutoMergeableMap(oldValue);

        value.forEach((k, v) -> mergedValue.merge(k, v, this::merge));
        return mergedValue;
    }

    private Object merge(Object oldValue, Object newValue) {
        if (oldValue instanceof Collection<?>) {
            return updateList((Collection<?>) oldValue, newValue);
        } else if (oldValue instanceof Map<?,?> && newValue instanceof Map<?,?>) {
            return updateMap(castObjectToMap(oldValue), castObjectToMap(newValue));
        } else {
            return newValue;
        }
    }

    private Map<String, Object> convertMap(Map<String, Object> map) {
        var collector = Collectors.<Entry<String, Object>, String, Object, AutoMergeableMap>toMap(
                Map.Entry::getKey,
                entry -> convertObject(entry.getValue()),
                (o1, o2) -> o1,
                AutoMergeableMap::new);

        return map.entrySet()
                .stream()
                .collect(collector);
    }

    private Object convertObject(Object obj) {
        if (obj instanceof Collection<?> && ! (obj instanceof HashSet<?>)) {
            return new HashSet<Object>((Collection<?>)obj);
        } else if (obj instanceof Map<?,?> && ! (obj instanceof AutoMergeableMap)) {
            return convertMap(castObjectToMap(obj));
        } else {
            return obj;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castObjectToMap(Object object) {
        return (Map<String, Object>) object;
    }
}
