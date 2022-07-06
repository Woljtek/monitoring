package eu.csgroup.coprs.monitoring.tracefilter.rule;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Data
@Slf4j
public class Rule implements Predicate<Object> {
    private final String key;
    private final String rawValue;

    private Pattern compiledValue;


    public Pattern compile() {
        if (compiledValue == null) {
            compiledValue = Pattern.compile(rawValue);
        }

        return compiledValue;
    }

    @Override
    public boolean test(Object value) {
        if (value instanceof Iterable<?>) {
            return StreamSupport.stream(((Iterable<?>)value).spliterator(), false)
                    .map(indexedValue -> compile().matcher(indexedValue.toString()).matches())
                    .reduce(true, (l,n) -> l & n);
        } else {
            return compile().matcher(value.toString()).matches();
        }
    }
}
