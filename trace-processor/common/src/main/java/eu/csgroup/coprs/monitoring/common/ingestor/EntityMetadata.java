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
    /**
     * Entity type class
     */
    private Class<? extends DefaultEntity> entityClass;

    /**
     * Entity type name
     */
    private String entityName;

    /**
     * This entity contains fields which are other entities
     */
    private Map<EntityMetadata, Deque<Field>> relyOn = new HashMap<>();

    /**
     * Entities that have this entity as a field
     */
    private Collection<Class<? extends DefaultEntity>> referencedBy = new HashSet<>();

    /**
     * Fields which are annotated with {@link javax.persistence.Column} and with attribute unique set to true
     */
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