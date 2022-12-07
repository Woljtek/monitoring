package eu.csgroup.coprs.monitoring.traceingestor.converter;

import org.springframework.beans.ConversionNotSupportedException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Class that handle the match action (only match one value at a time).<br>
 * <br>
 * Order and definition of required arguments is the following:
 * <ul>
 *  <li>STATIC: Pattern to match</li>
 *  <li>DYNAMIC: value to match to</li>
 * </ul>
 * <br><br>
 */
public class MatchAction extends Action {
    public MatchAction(String rawAction) {
        super(rawAction);
    }

    @Override
    protected List<Action.ARG_TYPE> getArgsMapping() {
        return List.of(Action.ARG_TYPE.STATIC, Action.ARG_TYPE.DYNAMIC);
    }

    @Override
    protected String getRequiredAction() {
        return ActionConstant.MATCH_ACTION_PATTERN;
    }

    @Override
    public Object execute(List<Object> values) {
        final var matcher = Pattern.compile(getAllArgs().get(0));
        final var value = values.get(0);

        try {
            return match(matcher, value) ? value : null;
        } catch (ConversionNotSupportedException | ClassCastException e) {
            // If format failed it may be caused by the value which is a list of value
            if (value instanceof final Collection<?> collection) {
                // Handle case where value is a list
                return collection.stream()
                        .map(val -> match(matcher, (String) val) ? val : null)
                        .filter(Objects::nonNull)
                        .toList();
            } else {
                throw e;
            }
        }

    }
    private boolean match(Pattern match, Object value) {
        if (value != null && match != null) {
            return match(match, (String)value);
        } else {
            return true;
        }
    }

    private boolean match(Pattern match, String value) {
        final var matcher = match.matcher(value);

        return matcher.find();
    }
}
