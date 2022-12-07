package eu.csgroup.coprs.monitoring.traceingestor.converter;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Base action which provide default mechanism to manipulate arguments.<br>
 * <br>
 * All arguments are associated to a type which is defined by the action (dynamic or static).
 * When defining arguments which are dynamic those which are in excess (more arguments given than required) will be tagged
 * with the same type as the last required. (see {@link #getDynamicArgs()} for an example).
 */
@Data
public class Action {
    /**
     * Enum to define argument's type.
     * Static argument type is a constant (can be an integer, float, regex, ....)
     * Dynamic argument type is described by an alias to indicate which value to use for this argument.
     */
    public enum ARG_TYPE {
        STATIC,
        DYNAMIC
    }

    /**
     * Action in string format
     */
    private final String rawAction;

    /**
     * Name of the action
     */
    private String actionName;

    /**
     * All arguments extracted from the raw action
     */
    private List<String> allArgs = new ArrayList<>();


    public Action(String rawConversion) {
        this.rawAction = rawConversion;
        build();
    }

    /**
     *
     * @return type and order of required argument
     */
    protected List<ARG_TYPE> getArgsMapping () {
        return List.of(ARG_TYPE.DYNAMIC);
    }

    /**
     *
     * @return Action signature
     */
    protected String getRequiredAction () {
        return ActionConstant.DEFAULT_ACTION_PATTERN;
    }

    /**
     * Return the list of dynamic argument according to order defined by {@link #getArgsMapping()} function.<br>
     * <br>
     * All argument in excess are typed by the last required argument type defined by {@link #getArgsMapping()} function:
     * <ul>
     *     <li>argument mapping: DYNAMIC STATIC DYNAMIC</li>
     *     <li>given argument: al1 al2 al3 al4 al5</li>
     * </ul>
     * argument al1, al3, al4 and al5 will be tagued as dynamic.
     *
     * @return list of dynamic argument.
     */
    public List<String> getDynamicArgs () {
        var currentArgType = ARG_TYPE.DYNAMIC;
        var argsMappingIt = getArgsMapping().iterator();
        var dynamicArgs = new ArrayList<String>();

        for (var arg : allArgs) {
            // Keep last argument type in cache
            if (argsMappingIt.hasNext()) {
                currentArgType = argsMappingIt.next();
            }

            // To tag all remaining argument with this type
            if (currentArgType == ARG_TYPE.DYNAMIC) {
                dynamicArgs.add(arg);
            }
        }

        // When the size of the given argument list is lower than the one required
        // set default alias name if last required type is dynamic
        while (argsMappingIt.hasNext()) {
            currentArgType = argsMappingIt.next();

            if (currentArgType == ARG_TYPE.DYNAMIC) {
                dynamicArgs.add("unknown");
            }
        }

        return dynamicArgs;
    }

    /**
     * Build the list of given argument by removing double quote encapsulation (not ones with a backslash)
     */
    private void build () {
        var splittedAction = rawAction.split(ActionConstant.ARGS_SEPARATOR);
        var dynamicArgsCount = getArgsMapping().stream()
                .filter(type -> type == ARG_TYPE.DYNAMIC)
                .count();

        // Remove action name for the comparison
        // Do not allow action when given argument list is lower than the one required.
        if (splittedAction.length - 1 < (getArgsMapping().size() - dynamicArgsCount)) {
            throw new InvalidActionException(
                    "Too few arguments given %s (require at least %s)".formatted(
                            rawAction,
                            getRequiredAction()
                    )
            );
        }

        actionName = splittedAction[0];

        // Re-construct argument containing whitespace by tracking double quote
        StringBuilder tempArg = new StringBuilder();
        for (var index = 1; index < splittedAction.length; index++) {
            // Add space that was removed with split function
            if (tempArg.length() > 0) {
                tempArg.append(ActionConstant.ARGS_SEPARATOR);
            }
            tempArg.append(splittedAction[index]);

            // Check if it's the end of the argument
            if (! tempArg.toString().contains("" + ActionConstant.ARG_ENCAPSULATION)
                    || hasBalancedQuote(tempArg.toString())) {

                allArgs.add(removeQuoteEncapsulation(tempArg.toString()));
                tempArg = new StringBuilder();
            }
        }

        if (tempArg.length() > 0) {
            allArgs.add(removeQuoteEncapsulation(tempArg.toString()));
        }

    }

    /**
     * Check if number of double quote is even in the given value.<br>
     * <br>
     * Do not take into account double quote associated to a backslash in the calculation.
     *
     * @param string Value to check in
     * @return true if number of double quote is even otherwise false.
     */
    private boolean hasBalancedQuote (String string) {
        // Get the total amount of double quote (including those which are associated to a backslash)
        final var quoteNumber = string.chars().filter(c -> c == ActionConstant.ARG_ENCAPSULATION).count();

        // Get the total amount of double quote which are only associated to a backslash
        boolean escapeDelimiter = false;
        var escapedQuoteNumber = 0;
        for (char c : string.toCharArray()) {
            if (c == ActionConstant.ESCAPE_DELIMITER) {
                escapeDelimiter = true;
            } else if (escapeDelimiter && c == ActionConstant.ARG_ENCAPSULATION) {
                escapedQuoteNumber++;
                escapeDelimiter = false;
            }
        }
        // Check if double quote number is even (without backslash)
        return (quoteNumber - escapedQuoteNumber) % 2 == 0;
    }

    /**
     * Remove double quote at the beginning and end of the stream.
     *
     * @param arg value where double quote must be removed (if any)
     * @return value without double quote otherwise original value
     */
    private String removeQuoteEncapsulation (String arg) {
        if (arg.indexOf(ActionConstant.ARG_ENCAPSULATION) == 0
                && arg.lastIndexOf(ActionConstant.ARG_ENCAPSULATION) == arg.length() - 1) {
            return arg.substring(1, arg.length() - 1);
        } else {
            return arg;
        }
    }

    /**
     * Execution the action
     *
     * @param values argument values list
     * @return result of the execution
     */
    public Object execute (List<Object> values) {
        return values.get(0);
    }
}
