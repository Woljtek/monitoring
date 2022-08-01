package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@PrimaryKeyJoinColumn(name="parent_id", referencedColumnName = "id")
public class Chunk extends ExternalInput {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "dsib_id", referencedColumnName = "parent_id")
    private Dsib dsib = new Dsib();

    @Override
    public Chunk copy() {
        return this.toBuilder().build();
    }

}
