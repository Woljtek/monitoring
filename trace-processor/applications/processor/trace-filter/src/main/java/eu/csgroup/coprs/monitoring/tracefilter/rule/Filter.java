package eu.csgroup.coprs.monitoring.tracefilter.rule;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.InvalidPropertyException;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
@Slf4j
public class Filter implements Predicate<BeanWrapper> {
    private String name;
    //private List<Rule> rules;
    private List<Rule> rules;


    public void setRules (Map<String, String> rules) {
        this.rules = new Vector<>();
        rules.entrySet().stream().forEach(entry -> {
            this.rules.add(new Rule(entry.getKey(), entry.getValue()));
        });
    }

    @Override
    public boolean test(BeanWrapper beanWrapper) {
        // Wrap rule check
        final Function<Rule, Boolean> applyRule = rule -> this.checkRule(rule, beanWrapper);

        return rules.stream()
                .map(applyRule)
                .reduce(true, (last, next) -> last & next);
    }

    private boolean checkRule(Rule rule, BeanWrapper beanWrapper) {
        var match = false;
        Object value = null;
        try {
            value = beanWrapper.getPropertyValue(rule.getKey());
            if (value != null) {
                match = rule.test(value);
            }
        } catch (InvalidPropertyException e) {
            log.trace(e.getMessage());
            match = false;
        }
        log.trace("Apply rule (path: %s; value: %s) on value '%s' => match: %s".formatted(rule.getKey(), rule.getRawValue(), value, match));

        return match;
    }
}
