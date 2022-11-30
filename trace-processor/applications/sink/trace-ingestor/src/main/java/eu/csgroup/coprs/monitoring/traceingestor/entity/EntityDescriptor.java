<<<<<<< HEAD
package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import lombok.*;

@NoArgsConstructor
@EqualsAndHashCode
public class EntityDescriptor {
    @Getter
    @Setter
    private EntityProcessing entityProcessing;


    /**
     * Indicate if entity as some value set
     */
    @Getter
    @Setter
    private boolean preFilled = false;

    /**
     * Tell if there is other entity to map
     */
    @Setter
    private boolean hasNext = false;

    public DefaultEntity getEntity () {
        return entityProcessing.getEntity();
    }

    public boolean hasNext() {
        return hasNext;
    }

}
||||||| b8aeece
=======
package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import lombok.*;

@NoArgsConstructor
@EqualsAndHashCode
public class EntityDescriptor<T extends DefaultEntity> {
    @Getter
    @Setter
    private BeanAccessor bean;

    /**
     * Tell which property must not be set/updated
     */
    @Getter
    @Setter
    /*private List<BeanProperty> lockedProperties = new Vector<>();*/
    /**
     * Indicate if entity as some value set
     */
    private boolean preFilled = false;

    /**
     * Tell if there is other entity to map
     */
    @Setter
    private boolean hasNext = false;

    public T getEntity () {
        return (T) bean.getDelegate().getWrappedInstance();
    }

    public boolean hasNext() {
        return hasNext;
    }

}
>>>>>>> dev
