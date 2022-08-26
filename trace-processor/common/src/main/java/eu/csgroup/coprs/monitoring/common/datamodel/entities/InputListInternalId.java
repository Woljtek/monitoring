package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@EqualsAndHashCode()
@ToString()
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class InputListInternalId implements Serializable {
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "processing_id")
    private Processing processing;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "product_id")
    private Product product;
}
