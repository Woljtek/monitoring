package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import eu.csgroup.coprs.monitoring.common.bean.AutoMergeableMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Product implements DefaultEntity, Serializable {
    @Transient
    @Serial
    private static final long serialVersionUID = -1088870334322071348L;


    @Id
    @SequenceGenerator(sequenceName="product_id_seq", name = "product_id_seq", allocationSize=1)
    @GeneratedValue(generator = "product_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String filename;

    @Type( type = "jsonb" )
    @Column(columnDefinition = "jsonb")
    private AutoMergeableMap custom;

    private String timelinessName;

    private int timelinessValueSeconds;

    private boolean endToEndProduct;

    private boolean duplicate;

    @Column(name = "t0_pdgs_date")
    private Instant t0PdgsDate;

    private Instant pripStorageDate;

    private boolean late;

    @Override
    public Product copy() {
        return this.toBuilder().build();
    }

    @Override
    public void resetId() {
        this.id = null;
    }
}
