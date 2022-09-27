package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode()
@ToString()
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class InputListExternalId implements Serializable {
    @Transient
    @Serial
    private static final long serialVersionUID = 678559889187487080L;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "external_input_id")
    private ExternalInput externalInput;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "processing_id")
    private Processing processing;
}
