package eu.csgroup.coprs.monitoring.common.bean;

import lombok.Data;
import org.springframework.beans.BeanWrapper;

@Data
public class BeanAccessor {
    private final BeanWrapper delegate;

    public void setPropertyValue (BeanProperty property, Object value) {
        delegate.setPropertyValue(property.getBeanPropertyPath(), value);
    }

    public Object getPropertyValue(BeanProperty property) {
        final var res = delegate.getPropertyValue(property.getBeanPropertyPath());
        return res;
    }


    public static BeanAccessor from(BeanWrapper delegate) {
        return new BeanAccessor(delegate);
    }
}
