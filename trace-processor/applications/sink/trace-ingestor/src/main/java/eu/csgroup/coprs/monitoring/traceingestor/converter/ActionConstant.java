package eu.csgroup.coprs.monitoring.traceingestor.converter;

public class ActionConstant {
    private ActionConstant () {

    }

    public static final String ARGS_SEPARATOR = " ";

    public static final char ARG_ENCAPSULATION = '"';

    public static final char ESCAPE_DELIMITER = '\\';

    public static final String DEFAULT_ACTION_PATTERN = "ACTION_NAME <arg>...";

    public static final String FORMAT_ACTION_PATTERN = "FORMAT <pattern> <formatter> <value>";

    public static final String MATCH_ACTION_PATTERN = "MATCH <pattern> <value>";

    public static final String SUBTRACT_ACTION_PATTERN = "SUBTRACT <value1> <value2>...";
}
