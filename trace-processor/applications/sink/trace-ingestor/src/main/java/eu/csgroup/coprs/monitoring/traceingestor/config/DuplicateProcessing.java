package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.bean.BeanPropertyRuleGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration class to define SQL query to use to process duplicate processing entity when a set of rules are validated
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DuplicateProcessing extends BeanPropertyRuleGroup {
    /**
     * SQL query start point to find duplicate entity (recursive)
     */
    private String query;

}
