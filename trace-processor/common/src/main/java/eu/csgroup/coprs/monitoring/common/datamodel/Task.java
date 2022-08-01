package eu.csgroup.coprs.monitoring.common.datamodel;

import com.fasterxml.jackson.annotation.*;
import eu.csgroup.coprs.monitoring.common.json.PropertyNames;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = BeginTask.class, name = "BEGIN"),
    @JsonSubTypes.Type(value = EndTask.class, name = "END") }
)
public class Task {
    @NotNull
    @Pattern(regexp = PropertyNames.UID_REGEX, message = "task.uid does not match UID pattern")
    private String uid;

    @NotNull
    @Size(max = 256, message = "Task name cannot exceed 256 characters")
    private String name;

    @NotNull
    private Event event;

    @JsonProperty("data_rate_mebibytes_sec")
    private double dataRateMebibytesSec;
    @JsonProperty("data_volume_mebibytes")
    private double dataVolumeMebibytes;

    private String satellite;

    @NotNull
    private Map input;
}
