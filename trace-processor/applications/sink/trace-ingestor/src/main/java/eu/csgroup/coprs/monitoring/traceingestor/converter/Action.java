package eu.csgroup.coprs.monitoring.traceingestor.converter;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Action {

    public enum ARG_TYPE {
        STATIC,
        DYNAMIC
    }

    private final String rawAction;

    private String actionName;

    private List<String> allArgs = new ArrayList<>();


    public Action(String rawConversion) {
        this.rawAction = rawConversion;
        build();
    }

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

    public List<String> getDynamicArgs () {
        var currentArgType = ARG_TYPE.DYNAMIC;
        var argsMappingIt = getArgsMapping().iterator();
        var dynamicArgs = new ArrayList<String>();

        for (var arg : allArgs) {
            if (argsMappingIt.hasNext()) {
                currentArgType = argsMappingIt.next();
            }

            if (currentArgType == ARG_TYPE.DYNAMIC) {
                dynamicArgs.add(arg);
            }
        }

        // Set default alias name when not set in configuration of the action.
        while (argsMappingIt.hasNext()) {
            currentArgType = argsMappingIt.next();

            if (currentArgType == ARG_TYPE.DYNAMIC) {
                dynamicArgs.add("unknown");
            }
        }

        return dynamicArgs;
    }

    private void build () {
        var splittedAction = rawAction.split(ActionConstant.ARGS_SEPARATOR);
        var dynamicArgsCount = getArgsMapping().stream()
                .filter(type -> type == ARG_TYPE.DYNAMIC)
                .count();

        // Remove action name for the comparison
        if (splittedAction.length - 1 < (getArgsMapping().size() - dynamicArgsCount)) {
            throw new InvalidActionException(
                    "Too few arguments given %s (require at least %s)".formatted(
                            rawAction,
                            getRequiredAction()
                    )
            );
        }

        actionName = splittedAction[0];

        StringBuilder tempArg = new StringBuilder();
        for (var index = 1; index < splittedAction.length; index++) {
            // Add space that was removed with split function
            if (tempArg.length() > 0) {
                tempArg.append(ActionConstant.ARGS_SEPARATOR);
            }
            tempArg.append(splittedAction[index]);

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

    private boolean hasBalancedQuote (String string) {
        final var quoteNumber = string.chars().filter(c -> c == ActionConstant.ARG_ENCAPSULATION).count();
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
        return (quoteNumber - escapedQuoteNumber) % 2 == 0;
    }

    private String removeQuoteEncapsulation (String arg) {
        if (arg.indexOf(ActionConstant.ARG_ENCAPSULATION) == 0
                && arg.lastIndexOf(ActionConstant.ARG_ENCAPSULATION) == arg.length() - 1) {
            return arg.substring(1, arg.length() - 1);
        } else {
            return arg;
        }
    }

    public Object execute (List<Object> values) {
        return values.get(0);
    }
}
