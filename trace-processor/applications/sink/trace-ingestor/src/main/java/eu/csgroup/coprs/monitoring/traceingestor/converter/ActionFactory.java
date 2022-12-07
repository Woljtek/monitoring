package eu.csgroup.coprs.monitoring.traceingestor.converter;

/**
 * Factory used to retrieve instance associated to the desired action among:
 * <ul>
 *     <li>MATCH: Check if a value match the regex</li>
 *     <li>FORMAT: Check if a value match the regex and create a new value based on capturing group</li>
 *     <li>SUBSTRACT:subtract two value</li>
 * </ul>
 */
public class ActionFactory {
    public static final String MATCH_ACTION = "MATCH";
    public static final String FORMAT_ACTION = "FORMAT";

    public static final String SUBSTRACT_ACTION = "SUBSTRACT";

    private ActionFactory() {

    }

    /**
     * Find and create instance associated to desired action
     *
     * @param rawAction the desired action
     * @return instance associated to desired action otherwise throw an exception if action is unknown
     * @throws InvalidActionException thrown when action is unknown
     */
    public static Action getConverter(String rawAction) {
        if (rawAction.startsWith(MATCH_ACTION)) {
            return new MatchAction(rawAction);
        } else if (rawAction.startsWith(FORMAT_ACTION)) {
            return new FormatAction(rawAction);
        } else if (rawAction.startsWith(SUBSTRACT_ACTION)) {
            return new SubstractAction(rawAction);
        } else {
            throw new InvalidActionException ("Action %s not found".formatted(rawAction));
        }
    }
}
