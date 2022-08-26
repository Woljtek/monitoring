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
public class InputListInternal extends DefaultEntity {

    @EmbeddedId
    private InputListInternalId id = new InputListInternalId();

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
