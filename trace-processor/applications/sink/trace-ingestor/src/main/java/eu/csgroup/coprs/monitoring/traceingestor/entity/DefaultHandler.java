package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.InstantPropertyEditor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.time.Instant;
import java.util.*;


@RequiredArgsConstructor
public class DefaultHandler<T extends DefaultEntity> {
    //private String entityName;

    @Getter
    private final Class<T> entityClass;

    private List<T> mergeableEntities;


    public void mergeWith(List<T> entities) {
        mergeableEntities = new Vector<>(entities);
    }


    public EntityDescriptor<T> getNextEntity() {
        final var entityDesc = new EntityDescriptor<T>();

        T entity = null;
        if (mergeableEntities != null && mergeableEntities.size() != 0) {
            entity = mergeableEntities.remove(0);
            //entityDesc.setLockedProperties(clauses);
            entityDesc.setPreFilled(true);
            entityDesc.setHasNext(! mergeableEntities.isEmpty());
        } else {
            // No more entity to merge with create default one
            entity = getDefaultEntity();
        }

        entityDesc.setBean(new BeanAccessor(getWrapper(entity)));

        return entityDesc;
    }

    public EntityDescriptor clone (T entity) {
        final var entityDesc = new EntityDescriptor();
        final var clone = EntityHelper.copy(entity);
        entityDesc.setBean(new BeanAccessor(getWrapper(clone)));

        return entityDesc;
    }

    /*public Class<T> getEntityClass() {
        if (className == null) {
            className = getEntityClass(entityName);
        }
        return className;
    }*/

    private T getDefaultEntity() {
        return createDefaultInstanceFor(entityClass);
    }

    private <E> E createDefaultInstanceFor(Class<E> className) {
        try {
            return className.getConstructor().newInstance();
        } catch (Exception e) {
            throw new EntityHandlerException("Unable to create default instance for class %s".formatted(className.getName()), e);
        }
    }

    private BeanWrapper getWrapper (T entity) {
        var wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        wrapper.setAutoGrowNestedPaths(true);
        wrapper.registerCustomEditor(Instant.class, new InstantPropertyEditor());

        return wrapper;
    }
}
