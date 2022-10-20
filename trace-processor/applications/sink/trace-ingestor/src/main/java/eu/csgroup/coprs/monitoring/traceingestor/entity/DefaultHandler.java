package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;


@RequiredArgsConstructor
public class DefaultHandler {

    @Getter
    private final Class<? extends DefaultEntity> entityClass;

    private List<EntityProcessing> mergeableEntities;


    public void mergeWith(List<EntityProcessing> entities) {
        mergeableEntities = new Vector<>(entities);
    }


    public EntityDescriptor getNextEntity() {
        final var entityDesc = new EntityDescriptor();

        EntityProcessing entity;
        if (mergeableEntities != null && ! mergeableEntities.isEmpty()) {
            entity = mergeableEntities.remove(0);
            entityDesc.setPreFilled(true);
            entityDesc.setHasNext(! mergeableEntities.isEmpty());
        } else {
            // No more entity to merge with create default one
            entity = getDefaultEntity();
        }

        entityDesc.setEntityProcessing(entity);

        return entityDesc;
    }

    public EntityDescriptor clone (DefaultEntity entity) {
        final var entityDesc = new EntityDescriptor();
        final var clone = EntityHelper.copy(entity, true);
        entityDesc.setEntityProcessing(EntityProcessing.fromEntity(clone));

        return entityDesc;
    }

    private EntityProcessing getDefaultEntity() {
        return EntityProcessing.fromEntity(
                createDefaultInstanceFor(entityClass)
        );
    }

    private <E> E createDefaultInstanceFor(Class<E> className) {
        try {
            return className.getConstructor().newInstance();
        } catch (Exception e) {
            throw new EntityHandlerException("Unable to create default instance for class %s".formatted(className.getName()), e);
        }
    }
}
