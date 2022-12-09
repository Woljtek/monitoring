package eu.csgroup.coprs.monitoring.common.bean;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Auto merge-able map which apply merge functionality (not based on {@link Map#merge(Object, Object, BiFunction)})
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
        // Check if a value is already stored for the given key
        if (containsKey(key)) {
            oldValue = get(key);

            // If so merge with the old one (result depend on value type)
            newValue = merge(oldValue, value);
        } else if (value instanceof Map<?,?> || value instanceof Collection<?>) {
            // convert into AutoMergeableMap (enable behavior for each level)
            // and collection into Set (avoid duplicate value)
            newValue = convertObject(value);
        }

        // Set in map only if old and new value diverge
        if (oldValue != newValue) {
            super.put(key, newValue);
        }
        return oldValue;
    }

    /**
     * Depending on the type of the new value set it in the old value by creating a new set.<br>
     * <br>
     * If new value is a list set each value individually in the set instead of setting the list a unique value of the
     * set.
     *
     * @param oldValue value already in map
     * @param value value to add (can be a single value or a list)
     * @return New set containing old and new value.
     */
    private Collection<?> updateList (Collection<?> oldValue, Object value) {
        final var mergedValue = new HashSet<Object>(oldValue);

        if (value instanceof Collection<?>) {
            mergedValue.addAll((Collection<?>) value);
        } else {
            mergedValue.add(value);
        }

        return mergedValue;
    }

    /**
     * Merge each value of the new map into the old one (see {@link #merge(Object, Object)} for merge details).
     *
     * @param oldValue value already in map
     * @param value value to add
     * @return new instance of the merge operation
     */
    private Map<String, Object> updateMap (Map<String, Object> oldValue, Map<String, Object> value) {
        final var mergedValue = new AutoMergeableMap(oldValue);

        value.forEach((k, v) -> mergedValue.merge(k, v, this::merge));
        return mergedValue;
    }

    /**
     * Depending on the type of the old value the new value is merged with the old value (case of set and map)
     * or the old value is replaced by the new one.
     *
     * @param oldValue value already in map
     * @param newValue new value to merge with the old one
     * @return Merge of the old and new value
     */
    private Object merge(Object oldValue, Object newValue) {
        if (oldValue instanceof Collection<?>) {
            return updateList((Collection<?>) oldValue, newValue);
        } else if (oldValue instanceof Map<?,?> && newValue instanceof Map<?,?>) {
            return updateMap(castObjectToMap(oldValue), castObjectToMap(newValue));
        } else {
            return newValue;
        }
    }

    /**
     * Convert the given value deeply to replace {@link Collection} into {@link Set} and {@link Map} into {@link AutoMergeableMap}
     *
     * @param map Map to convert
     * @return converted map
     */
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

    /**
     * Convert the object into the right type. Handled type are:
     * <ul>
     *     <li>{@link Collection} converted into {@link Set}</li>
     *     <li>{@link Map} converted into {@link AutoMergeableMap}</li>
     * </ul>
     * For other type no conversion is done, the value is kept as is.
     *
     * @param obj Object to convert
     * @return Converted value or given object if it's not a supported type
     */
    private Object convertObject(Object obj) {
        if (obj instanceof Collection<?> && ! (obj instanceof HashSet<?>)) {
            return new HashSet<Object>((Collection<?>)obj);
        } else if (obj instanceof Map<?,?> && ! (obj instanceof AutoMergeableMap)) {
            return convertMap(castObjectToMap(obj));
        } else {
            return obj;
        }
    }

    /**
     * Utility function to isolate the uncheck cast
     *
     * @param object Object to cast into map
     * @return cast object
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castObjectToMap(Object object) {
        return (Map<String, Object>) object;
    }
}