package eu.csgroup.coprs.monitoring.common.datamodel.entities;

public interface ClonableEntity<T> {
    T copy();

    void setId(Long id);
}
