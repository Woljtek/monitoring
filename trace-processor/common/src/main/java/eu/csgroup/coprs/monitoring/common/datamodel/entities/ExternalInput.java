package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import eu.csgroup.coprs.monitoring.common.bean.AutoMergeableMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import java.time.Instant;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Inheritance(strategy = InheritanceType.JOINED)
@Embeddable
public class ExternalInput extends DefaultEntity {
    @Id
    @SequenceGenerator(sequenceName="external_input_id_seq", name = "external_input_id_seq", allocationSize=1)
    @GeneratedValue(generator = "external_input_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String filename;

    private String mission;

    private Instant pickupPointSeenDate;

    private Instant pickupPointAvailableDate;

    private Instant ingestionDate;

    private Instant catalogStorageDate;

    @Type( type = "jsonb" )
    @Column(columnDefinition = "jsonb")
    private AutoMergeableMap custom;

    @Override
    public Object copy() {
        return this.toBuilder().build();
    }
}
