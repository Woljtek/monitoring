package eu.csgroup.coprs.monitoring.traceingestor.converter;

import org.springframework.beans.ConversionNotSupportedException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Class that handle the format action (only format one value at a time).<br>
 * <br>
 * Order and definition of required arguments is the following:
 * <ul>
 *  <li>STATIC: Pattern to match containing capturing group</li>
 *  <li>STATIC: Format pattern of the value</li>
 *  <li>DYNAMIC: value to format</li>
 * </ul>
 * <br><br>
 * To use a capturing group in the format pattern use % followed by the number of the desired capturing group
 * (see {@link Pattern} to identify the number of the desired capturing group).<br>
 * <br>
 * To format value contained in a capturing group see {@link java.util.Formatter}
 */
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
            // If format failed it may be caused by the value which is a list of value
            if (value instanceof final Collection<?> collection) {
                // Handle case where value is a list
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
            // Get capturing group
            int count = 0;
            final var groupCount = matcher.groupCount();
            final var groups = new LinkedList<String>();
            while (count <= groupCount) {
                groups.add(matcher.group(count++));
            }

            // Then format the value according to given pattern
            return replace.formatted(groups.toArray(String[]::new));
        } else {
            return null;
        }
    }
}
