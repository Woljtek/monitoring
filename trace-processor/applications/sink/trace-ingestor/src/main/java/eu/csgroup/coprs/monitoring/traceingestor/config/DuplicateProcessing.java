package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.bean.BeanPropertyRuleGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DuplicateProcessing extends BeanPropertyRuleGroup {
    /**
     * SQL query start point to find duplicate entity (recursive)
     */
    private String query;

}
