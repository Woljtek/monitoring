package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import lombok.*;

@NoArgsConstructor
@EqualsAndHashCode
public class EntityDescriptor {
    @Getter
    @Setter
    private BeanAccessor bean;


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
        return (DefaultEntity) bean.getDelegate().getWrappedInstance();
    }

    public boolean hasNext() {
        return hasNext;
    }

}
