package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Field;
import java.util.*;

@Getter
@Setter
@ToString
public class EntityMetadata {
    private Class<? extends DefaultEntity> entityClass;

    private String entityName;

    private Map<EntityMetadata, Deque<Field>> relyOn = new HashMap<>();

    private Collection<Class<? extends DefaultEntity>> referencedBy = new HashSet<>();

    private Collection<Field> dependencies = new HashSet<>();

    /**
     * Polymorphism
     */
    private Collection<Class<? extends DefaultEntity>> child = new HashSet<>();

    public void addChild (Class<? extends DefaultEntity> entityClass) {
        child.add(entityClass);
    }

    public void addReferencedBy (Class<? extends DefaultEntity> entityClass) {
        referencedBy.add(entityClass);
    }
}
