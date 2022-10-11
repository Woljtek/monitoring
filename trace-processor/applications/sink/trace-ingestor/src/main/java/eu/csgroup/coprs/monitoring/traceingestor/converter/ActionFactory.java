package eu.csgroup.coprs.monitoring.traceingestor.converter;

public class ActionFactory {
    public static final String MATCH_ACTION = "MATCH";
    public static final String FORMAT_ACTION = "FORMAT";

    public static final String SUBSTRACT_ACTION = "SUBSTRACT";

    private ActionFactory() {

    }

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
