package eu.csgroup.coprs.monitoring.common.bean;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * Define a rule to apply for a given bean property path. Result of the test is true when the value extracted for the given
 * path match to the pattern otherwise return false.<br>
 * <br>
 * For list value, test is done on each value of the list individually. Result is the combination of all test with the
 * logical and operator
 */
@Data
@Slf4j
public class BeanPropertyRule implements Predicate<Object> {
    /**
     * Bean property path
     */
    private final BeanProperty property;
    /**
     * constant or regex
     */
    private final String rawValue;

    /**
     * Pattern of {@link #rawValue}
     */
    private Pattern compiledValue;

    /**
     * Create pattern
     *
     * @return pattern
     */
    public Pattern compile() {
        if (compiledValue == null) {
            compiledValue = Pattern.compile(rawValue);
        }

        return compiledValue;
    }

    /**
     * Check if the value match to the pattern. If value is a list check value individually and the result will be the
     * combination of logical and operator
     *
     * @param value the input argument
     * @return true if value match to the pattern otherwise false
     */
    @Override
    public boolean test(Object value) {
        // Apply pattern to each value of the list
        if (value instanceof Iterable<?>) {
            return StreamSupport.stream(((Iterable<?>)value).spliterator(), false)
                    .map(indexedValue -> compile().matcher(indexedValue.toString()).matches())
                    .reduce(true, (l,n) -> l && n);
        } else {
            return compile().matcher(value.toString()).matches();
        }
    }
}
