package eu.csgroup.coprs.monitoring.common.ingestor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Field;
import java.util.*;

@Getter
@Setter
@ToString
public class EntityMetadata {
    private Class entityClass;

    private String entityName;

    private Map<EntityMetadata, Deque<Field>> relyOn = new HashMap<>();

    private Collection<Class> referencedBy = new HashSet<>();

    private Collection<Field> dependencies = new HashSet<>();

    /**
     * Polymorphism
     */
    private Collection<Class> child = new HashSet<>();

    public void addChild (Class entityClass) {
        child.add(entityClass);
    }

    public void addReferencedBy (Class entityClass) {
        referencedBy.add(entityClass);
    }

    public void addDependency (Field dependency) {
        dependencies.add(dependency);
    }
}
