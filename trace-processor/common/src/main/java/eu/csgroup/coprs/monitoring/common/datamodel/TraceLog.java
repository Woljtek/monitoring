package eu.csgroup.coprs.monitoring.common.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TraceLog {

    @JsonProperty("@timestamp")
    private double timestamp;

    //@JsonFormat(shape=JsonFormat.Shape.STRING, pattern= PropertyNames.TRACE_LOG_TIME_PATTERN, timezone = PropertyNames.DEFAULT_TIMEZONE)
    private String time;

    private String stream;

    @JsonProperty("_p")
    private String p;

    @Valid
    @JsonProperty("log")
    private Trace trace;

    private Map kubernetes;
}
