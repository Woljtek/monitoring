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

    private String time;

    private String stream;

    @JsonProperty("_p")
    private String p;

    @Valid
    @JsonProperty("log")
    private Trace trace;

    private Map<String, Object> kubernetes;
}
