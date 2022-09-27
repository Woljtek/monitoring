package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import eu.csgroup.coprs.monitoring.common.datamodel.Status;
import eu.csgroup.coprs.monitoring.common.datamodel.Level;
import eu.csgroup.coprs.monitoring.common.datamodel.Workflow;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@EqualsAndHashCode()
@ToString()
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@TypeDef(name="pgsql_enum", typeClass =  PostgreSQLEnumType.class)
@Embeddable
public class Processing implements DefaultEntity, Serializable {
    @Transient
    @Serial
    private static final long serialVersionUID = -311807617227639758L;


    @Id
    @SequenceGenerator(sequenceName="external_input_id_seq", name = "external_input_id_seq", allocationSize=1)
    @GeneratedValue(generator = "external_input_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    private String mission;

    @Column(name="rs_chain_name")
    private String rsChainName;

    @Column(name="rs_chain_version")
    private String rsChainVersion;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private Level level;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private Status status;

    @Column(name="processing_date")
    private Instant processingDate;

    @Column(name="end_sensing_date")
    private Instant endSensingDate;

    @Column(name="t0_pdgs_date")
    private Instant t0PdgsDate;

    private boolean duplicate;


    @Override
    public Processing copy() {
        return this.toBuilder().build();
    }

    @Override
    public void resetId() {
        this.id = null;
    }
}
