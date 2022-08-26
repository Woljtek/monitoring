package eu.csgroup.coprs.monitoring.common.bean;

import lombok.Data;
import org.springframework.beans.BeanWrapper;

import java.util.HashMap;
import java.util.Map;

@Data
public class BeanAccessor {
    private final BeanWrapper delegate;

    private final Map<String, Object> cache = new HashMap<>();

    public void setPropertyValue (BeanProperty property, Object value) {
        delegate.setPropertyValue(property.getBeanPropertyPath(), value);
        cache.put(property.getBeanPropertyPath(), value);
    }

    public Object getPropertyValue(BeanProperty property) {
        Object res = null;
        if (! cache.containsKey(property.getBeanPropertyPath())) {
            res = delegate.getPropertyValue(property.getBeanPropertyPath());
            cache.put(property.getBeanPropertyPath(), res);
        } else {
            res = cache.get(property.getBeanPropertyPath());
        }

        return res;
    }


    public static BeanAccessor from(BeanWrapper delegate) {
        return new BeanAccessor(delegate);
    }
}
