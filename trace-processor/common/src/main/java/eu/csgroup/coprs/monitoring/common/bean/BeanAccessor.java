package eu.csgroup.coprs.monitoring.common.bean;

import lombok.*;
import org.springframework.beans.BeanWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper of {@link BeanWrapper} instance to support use of {@link BeanProperty} to access value in the bean with
 * {@link BeanWrapper} instance.
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class BeanAccessor {
    @Getter
    private final BeanWrapper delegate;

    @Getter
    private final Map<String, Object> cache = new HashMap<>();


    /**
     * Set value in bean whatever value is (even if it's the same value than the one in the bean). Set value in cache
     * for later access.
     *
     * @param property Define the field for which to put the value.
     * @param value value to set.
     */
    public void setPropertyValue (BeanProperty property, Object value) {
        delegate.setPropertyValue(property.getBeanPropertyPath(), value);
        cache.put(property.getBeanPropertyPath(), value);
    }

    /**
     * Get value for the defined field by first accessing to the cache (if already in) or get it directly in the bean.
     * If value is retrieved from the bean, set it in cache.
     *
     * @param property Define the field for which to get the value
     * @return retrieved value.
     */
    public Object getPropertyValue(BeanProperty property) {
        Object res;
        if (! cache.containsKey(property.getBeanPropertyPath())) {
            res = delegate.getPropertyValue(property.getBeanPropertyPath());
            cache.put(property.getBeanPropertyPath(), res);
        } else {
            res = cache.get(property.getBeanPropertyPath());
        }

        return res;
    }

    /**
     * Utility class to create the wrapper from the instance that access directly to the bean.
     *
     * @param delegate Instance that access directly to the bean
     * @return wrapper
     */
    public static BeanAccessor from(BeanWrapper delegate) {
        return new BeanAccessor(delegate);
    }
}