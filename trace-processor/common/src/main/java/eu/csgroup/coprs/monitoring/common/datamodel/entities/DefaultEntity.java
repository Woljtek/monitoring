<<<<<<< HEAD
package eu.csgroup.coprs.monitoring.common.datamodel.entities;


public interface DefaultEntity {
    DefaultEntity copy();

    void resetId();
}
||||||| b8aeece
=======
package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
public abstract class DefaultEntity implements ClonableEntity {
}
>>>>>>> dev
