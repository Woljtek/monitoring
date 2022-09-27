package eu.csgroup.coprs.monitoring.common.datamodel.entities;


public interface DefaultEntity {
    DefaultEntity copy();

    void resetId();
}
