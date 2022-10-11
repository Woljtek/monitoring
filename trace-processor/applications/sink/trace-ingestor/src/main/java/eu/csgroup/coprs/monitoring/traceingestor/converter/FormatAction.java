package eu.csgroup.coprs.monitoring.traceingestor.converter;

import org.springframework.beans.ConversionNotSupportedException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class FormatAction extends Action {

    public FormatAction(String rawAction) {
        super(rawAction);
    }

    @Override
    protected List<ARG_TYPE> getArgsMapping() {
        return List.of(ARG_TYPE.STATIC, ARG_TYPE.STATIC, ARG_TYPE.DYNAMIC);
    }

    @Override
    protected String getRequiredAction() {
        return ActionConstant.FORMAT_ACTION_PATTERN;
    }

    @Override
    public Object execute(List<Object> values) {
        final var matcher = Pattern.compile(getAllArgs().get(0));
        final var formatter = getAllArgs().get(1);
        final var value = values.get(0);

        try {
            return format(matcher, formatter, value);
        } catch (ConversionNotSupportedException | ClassCastException e) {
            if (value instanceof final Collection<?> collection) {
                return collection.stream()
                        .map(val -> format(
                                matcher,
                                formatter,
                                (String) val)
                        ).filter(Objects::nonNull)
                        .toList();
            } else {
                throw e;
            }
        }

    }
    private Object format(Pattern match, String replace, Object value) {
        if (value != null && match != null) {
            return format(match, replace, (String)value);
        } else {
            return value;
        }
    }

    private String format(Pattern match , String replace, String value) {
        final var matcher = match.matcher(value);

        if (matcher.find()) {
            int count = 0;
            final var groupCount = matcher.groupCount();
            final var groups = new LinkedList<String>();
            while (count <= groupCount) {
                groups.add(matcher.group(count++));
            }

            return replace.formatted(groups.toArray(String[]::new));
        } else {
            return null;
        }
    }
}
