package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class InputListExternal extends DefaultEntity {

    @EmbeddedId
    private InputListExternalId id = new InputListExternalId();

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
