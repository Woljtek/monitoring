package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;

import javax.persistence.*;

@Data
@EqualsAndHashCode()
@ToString()
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class InputListExternal implements DefaultEntity {

    @EmbeddedId
    private InputListExternalId id = new InputListExternalId();

    @Override
    public InputListExternal copy() {
        return this.toBuilder()
                .id(this.id.toBuilder().build())
                .build();
    }

    @Override
    public void resetId() {
        this.id = new InputListExternalId();
    }
}
