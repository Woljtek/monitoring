package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.InstantPropertyEditor;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.time.Instant;
import java.util.*;


@RequiredArgsConstructor
public class DefaultHandler<T extends ExternalInput> {
    private final String entityName;

    private Class<T> className;

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
        final var clone = (T)entity.copy();
        // Avoid erasing an existing entity in repository with wrong value
        clone.setId(null);
        entityDesc.setBean(new BeanAccessor(getWrapper(clone)));

        return entityDesc;
    }

    public Class<T> getEntityClass() {
        if (className == null) {
            try {
                className = (Class<T>) Class.forName("%s.%s".formatted(EntityIngestor.BASE_PACKAGE, entityName));
            } catch (ClassNotFoundException e) {
                throw new EntityHandlerException("Entity with name %s does not exists".formatted(entityName), e);
            }
        }
        return className;
    }

    private T getDefaultEntity() {
        return createDefaultInstanceFor(getEntityClass());
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
