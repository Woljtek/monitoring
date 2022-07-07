package eu.csgroup.coprs.monitoring.common.message;

import eu.csgroup.coprs.monitoring.common.datamodel.Trace;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilteredTrace {
    private String ruleName;
    private Trace trace;
}
