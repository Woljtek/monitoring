package eu.csgroup.coprs.monitoring.common.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EndTask extends Task {
    @NotNull
    private Status status;

    @NotNull
    @JsonProperty("error_code")
    private int errorCode;

    @NotNull
    @JsonProperty("duration_in_seconds")
    private Double durationInSeconds;

    @NotNull
    private Map<String, Object> output;

    @NotNull
    private Map<String, Object> quality;

    @JsonProperty("missing_output")
    private Collection<Object> missingOutput;
}
