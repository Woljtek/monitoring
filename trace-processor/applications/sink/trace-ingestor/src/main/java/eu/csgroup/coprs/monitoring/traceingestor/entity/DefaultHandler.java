package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.InstantPropertyEditor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.PropertyAccessorFactory;

import java.time.Instant;
import java.util.*;


@RequiredArgsConstructor
public class DefaultHandler {

    @Getter
    private final Class<DefaultEntity> entityClass;

    private List<DefaultEntity> mergeableEntities;


    public void mergeWith(List<DefaultEntity> entities) {
        mergeableEntities = new Vector<>(entities);
    }


    public EntityDescriptor getNextEntity() {
        final var entityDesc = new EntityDescriptor();

        DefaultEntity entity;
        if (mergeableEntities != null && ! mergeableEntities.isEmpty()) {
            entity = mergeableEntities.remove(0);
            entityDesc.setPreFilled(true);
            entityDesc.setHasNext(! mergeableEntities.isEmpty());
        } else {
            // No more entity to merge with create default one
            entity = getDefaultEntity();
        }

        entityDesc.setBean(getWrapper(entity));

        return entityDesc;
    }

    public EntityDescriptor clone (DefaultEntity entity) {
        final var entityDesc = new EntityDescriptor();
        final var clone = EntityHelper.copy(entity, true);
        entityDesc.setBean(getWrapper(clone));

        return entityDesc;
    }

    private DefaultEntity getDefaultEntity() {
        return createDefaultInstanceFor(entityClass);
    }

    private <E> E createDefaultInstanceFor(Class<E> className) {
        try {
            return className.getConstructor().newInstance();
        } catch (Exception e) {
            throw new EntityHandlerException("Unable to create default instance for class %s".formatted(className.getName()), e);
        }
    }

    public BeanAccessor getWrapper (DefaultEntity entity) {
        var wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        wrapper.setAutoGrowNestedPaths(true);
        wrapper.registerCustomEditor(Instant.class, new InstantPropertyEditor());

        return new BeanAccessor(wrapper);
    }
}
