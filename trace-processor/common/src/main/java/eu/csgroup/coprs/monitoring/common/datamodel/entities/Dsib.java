package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.bytebuddy.implementation.bind.annotation.Super;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@Entity
@PrimaryKeyJoinColumn(name="parent_id", referencedColumnName = "id")
public class Dsib extends ExternalInput {
    @Override
    public Dsib copy() {
        return this.toBuilder().build();
    }
}
