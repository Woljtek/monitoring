package eu.csgroup.coprs.monitoring.common.datamodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    @NotNull
    @Size(max= Properties.STRING_FIELD_10K_LIMIT, message="Message content cannot exceed "+ Properties.STRING_FIELD_10K_LIMIT +" characters")
    private String content;

}
