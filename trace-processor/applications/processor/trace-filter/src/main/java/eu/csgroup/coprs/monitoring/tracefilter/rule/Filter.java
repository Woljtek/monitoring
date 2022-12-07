package eu.csgroup.coprs.monitoring.tracefilter.rule;

import eu.csgroup.coprs.monitoring.common.bean.BeanPropertyRuleGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;


@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Filter extends BeanPropertyRuleGroup {
    private String name;

}
