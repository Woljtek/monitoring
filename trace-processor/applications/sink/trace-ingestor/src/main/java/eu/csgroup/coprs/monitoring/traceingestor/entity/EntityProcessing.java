package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.bean.InstantPropertyEditor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.time.Instant;

/**
 * Extends {@link BeanAccessor} to add a processing state on the processed entity.<br>
 * <br>
 * The state principally indicate if the entity is newly created, updated (retrieved from storage and at least one field was updated)
 * or unchanged (retrieved from storage without field updated).
 */
@EqualsAndHashCode(callSuper = true)
public class EntityProcessing extends BeanAccessor {
    @Getter
    @Setter
    private EntityState state;

    /**
     * Create object with wrapped entity to process and setting it's state to {@link EntityState#NEW}
     * @param bean Wrapped entity
     */
    public EntityProcessing (BeanWrapper bean) {
        this(bean, EntityState.NEW);
    }

    public EntityProcessing (BeanWrapper bean, EntityState state) {
        super(bean);
        this.state = state;
    }

    @Override
    public void setPropertyValue(BeanProperty property, Object value) {
        var object = this.getPropertyValue(property);

        if ((object != null && ! object.equals(value)) || value != null) {
            state = EntityState.UPDATED;
        }

        super.setPropertyValue(property, value);
    }

    public DefaultEntity getEntity () {
        return (DefaultEntity) getDelegate().getWrappedInstance();
    }

    public static BeanWrapper getWrapper (DefaultEntity entity) {
        var wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        wrapper.setAutoGrowNestedPaths(true);
        wrapper.registerCustomEditor(Instant.class, new InstantPropertyEditor());

        return wrapper;
    }

    /**
     * Static class to create instance for the entity to process.<br>
     * <br>
     * By default, the state is set to new
     *
     * @param entity Entity to process
     * @return {@link EntityProcessing} instance for the given entity
     */
    public static EntityProcessing fromEntity(DefaultEntity entity) {
        return new EntityProcessing(getWrapper(entity));
    }

    /**
     * Static class to create instance with the given state for the entity to process
     *
     * @param entity Entity to process
     * @param state Start state to set
     * @return {@link EntityProcessing} instance for the given entity
     */
    public static EntityProcessing fromEntity(DefaultEntity entity, EntityState state) {
        return new EntityProcessing(getWrapper(entity), state);
    }
}
