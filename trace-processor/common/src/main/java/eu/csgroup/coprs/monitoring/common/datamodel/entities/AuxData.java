package eu.csgroup.coprs.monitoring.common.datamodel.entities;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder(toBuilder = true)
@AllArgsConstructor
@TypeDefs({
        @TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class),
        @TypeDef(name = "json", typeClass = JsonType.class)
})
@Entity
@PrimaryKeyJoinColumn(name="parent_id", referencedColumnName = "id")
public class AuxData extends ExternalInput {

    public AuxData copy () {
        return this.toBuilder().build();
    }
}
