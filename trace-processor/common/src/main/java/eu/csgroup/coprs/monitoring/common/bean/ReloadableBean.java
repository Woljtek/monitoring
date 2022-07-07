package eu.csgroup.coprs.monitoring.common.bean;

public interface ReloadableBean {
    boolean isReloadNeeded();

    void setReloaded();
}
