package eu.csgroup.coprs.monitoring.traceingestor.converter;

/**
 * Constants used in action class
 */
public class ActionConstant {
    private ActionConstant () {

    }

    /**
     * Separator used between each argument (and also action name and the first argument)
     */
    public static final String ARGS_SEPARATOR = " ";

    /**
     * Character used to encapsulate argument containing whitespace
     */
    public static final char ARG_ENCAPSULATION = '"';

    /**
     * Character to use to indicate that this is not a {@link #ARG_ENCAPSULATION}
     */
    public static final char ESCAPE_DELIMITER = '\\';

    /**
     * Default signature of the base action
     */
    public static final String DEFAULT_ACTION_PATTERN = "ACTION_NAME <arg>...";

    /**
     * Signature of the format action
     */
    public static final String FORMAT_ACTION_PATTERN = "FORMAT <pattern> <formatter> <value>";

    /**
     * Signature of the match action
     */
    public static final String MATCH_ACTION_PATTERN = "MATCH <pattern> <value>";

    /**
     * Signature of the subtract action
     */
    public static final String SUBSTRACT_ACTION_PATTERN = "SUBSTRACT <value1> <value2>...";
}
