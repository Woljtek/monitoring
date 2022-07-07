package eu.csgroup.coprs.monitoring.common.datamodel;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Trace {
    @NotNull
    @Valid
    private Header header;

    @NotNull
    @Valid
    private Message message;

    @Valid
    private Task task;

    private Map custom;
}
