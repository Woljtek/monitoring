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
public class InputListExternalId implements Serializable {
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "external_input_id")
    private ExternalInput externalInput;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "processing_id")
    private Processing processing;
}
