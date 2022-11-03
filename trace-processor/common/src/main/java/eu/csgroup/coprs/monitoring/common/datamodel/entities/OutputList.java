<<<<<<< HEAD
package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Data
@EqualsAndHashCode()
@ToString()
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class OutputList implements DefaultEntity {

    @EmbeddedId
    private OutputListId id = new OutputListId();

    @Override
    public OutputList copy() {
        return this.toBuilder()
                .id(this.id.toBuilder().build())
                .build();
    }

    @Override
    public void resetId() {
        this.id = new OutputListId();
    }
}
||||||| b8aeece
=======
package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class OutputList extends DefaultEntity {

    @EmbeddedId
    private OutputListId id = new OutputListId();

    @Override
    public Object copy() {
        return this.toBuilder()
                .id(this.id.toBuilder().build())
                .build();
    }

    @Override
    public void setId(Long id) {

    }
}
>>>>>>> dev
