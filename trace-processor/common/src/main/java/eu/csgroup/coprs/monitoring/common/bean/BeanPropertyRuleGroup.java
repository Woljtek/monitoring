package eu.csgroup.coprs.monitoring.common.bean;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.InvalidPropertyException;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Accumulation of {@link BeanPropertyRule} to test a set of rule on a bean. The result of each rule is reduces into one
 * by the use of the logical and operator. So if at least one rule is not passed the final result is false.<br>
 * <br>
 * If the instance does not contain any rule the result of the test on a given bean will always be true.
 */
@Data
@Slf4j
public class BeanPropertyRuleGroup implements Predicate<BeanAccessor> {
    /**
     * Set of rule to test on a given bean
     */
    private List<BeanPropertyRule> rules;


    /**
     * Convert a map into a set {@link BeanPropertyRule} (where key is the bean property path and value is the rule to apply
     * for the test)
     *
     * @param rules set of rules
     */
    public void setRules (Map<String, String> rules) {
        this.rules = new Vector<>();
        rules.forEach((key, value) -> this.rules.add(
                new BeanPropertyRule(new BeanProperty(
                        key),
                        value
                )
        ));
    }

    @Override
    public boolean test(BeanAccessor beanAccessor) {
        // Wrap rule check
        final Function<BeanPropertyRule, Boolean> applyRule = rule -> this.checkRule(rule, beanAccessor);

        if (rules == null || rules.isEmpty()) {
            return true;
        }

        // Test bean on each rule
        return rules.stream()
                .map(applyRule)
                .reduce(true, (last, next) -> last && next);
    }

    /**
     * Apply the given rule on the given bean.
     *
     * @param rule rule to apply on bean
     * @param beanAccessor Accessor of the bean
     * @return true if bean match the rule otherwise false or if value is null.
     */
    private boolean checkRule(BeanPropertyRule rule, BeanAccessor beanAccessor) {
        var match = false;
        Object value = null;
        try {
            // Get the value in the bean defined by the bean property path
            value = beanAccessor.getPropertyValue(rule.getProperty());
            if (value != null) {
                log.trace("Compare %s %s to configured value %s".formatted(value, rule.getProperty(), rule.getRawValue()));
                match = rule.test(value);
            } else {
                log.trace("No value found %s".formatted(rule.getProperty()));
            }
        } catch (InvalidPropertyException e) {
            log.trace(e.getMessage());
        }
        log.trace("Apply rule (path: %s; value: %s) on value '%s' => match: %s".formatted(rule.getProperty().getBeanPropertyPath(true), rule.getRawValue(), value, match));

        return match;
    }
}
