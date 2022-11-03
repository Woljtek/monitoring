package eu.csgroup.coprs.monitoring.common.datamodel;

import eu.csgroup.coprs.monitoring.common.json.PropertyNames;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    @NotNull
    @Size(max= PropertyNames.STRING_FIELD_10K_LIMIT, message="Message content cannot exceed "+PropertyNames.STRING_FIELD_10K_LIMIT +" characters")
    private String content;

}
