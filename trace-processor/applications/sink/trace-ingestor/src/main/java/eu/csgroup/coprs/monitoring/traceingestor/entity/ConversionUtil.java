package eu.csgroup.coprs.monitoring.traceingestor.entity;


import java.util.LinkedList;
import java.util.regex.Pattern;

public final class ConversionUtil {

    private ConversionUtil () {

    }

    public static Object convert(Pattern match, String replace, Object value) {
        if (value != null && match != null) {
            return convert(match, replace, (String)value);
        } else {
            return value;
        }
    }

    public static String convert(Pattern match , String replace, String value) {
        final var matcher = match.matcher(value);

        if (replace == null) {
            return matcher.matches() ? value : null;
        } else if (matcher.find()) {
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
