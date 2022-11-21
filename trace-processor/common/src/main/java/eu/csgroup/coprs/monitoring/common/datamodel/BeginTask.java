package eu.csgroup.coprs.monitoring.common.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.csgroup.coprs.monitoring.common.json.PropertyNames;
import lombok.*;

import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BeginTask extends Task {
    @JsonProperty("child_of_task")
    @Pattern(regexp = PropertyNames.UID_REGEX, message = "Child uid of task does not match UID pattern")
    private String childOfTask;

    @JsonProperty("follows_from_task")
    @Pattern(regexp = PropertyNames.UID_REGEX, message = "Follow uid from task does not match UID pattern")
    private String followsFromTask;
}
