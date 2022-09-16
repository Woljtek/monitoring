package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import eu.csgroup.coprs.monitoring.common.bean.AutoMergeableMap;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class MissingProducts extends DefaultEntity {

    @Id
    @SequenceGenerator(sequenceName="missing_products_id_seq", name = "missing_products_id_seq", allocationSize=1)
    @GeneratedValue(generator = "missing_products_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "processing_failed_id", referencedColumnName = "id")
    private Processing processing = new Processing();

    @Type( type = "jsonb" )
    @Column(columnDefinition = "jsonb")
    private AutoMergeableMap productMetadataCustom;

    private Integer estimatedCount;

    private boolean duplicate;

    private boolean endToEndProduct;


    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public MissingProducts copy() {
        return this.toBuilder().build();
    }

}
