package eu.csgroup.coprs.monitoring.common.datamodel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.csgroup.coprs.monitoring.common.json.PropertyNames;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Header {

    @NotNull
    private TraceType type;

    @NotNull
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern= PropertyNames.DATE_PATTERN, timezone = PropertyNames.DEFAULT_TIMEZONE)
    private Instant timestamp;

    @NotNull
    private TraceLevel level;

    @NotNull
    private Mission mission;

    @JsonProperty("rs_chain_name")
    @Size(max= PropertyNames.STRING_FIELD_256_LIMIT, message="header.rs_chain_name cannot exceed " + PropertyNames.STRING_FIELD_10K_LIMIT + " characters")
    private String rsChainName;

    @JsonProperty("rs_chain_version")
    @Pattern(regexp = PropertyNames.VERSION_REGEX, message = "header.rs_chain_version does not match UID pattern")
    private String rsChainVersion;

    private Workflow workflow;

    @JsonProperty("debug_mode")
    Boolean debugMode;

    @JsonProperty("tag_list")
    List<String> tagList;
}
