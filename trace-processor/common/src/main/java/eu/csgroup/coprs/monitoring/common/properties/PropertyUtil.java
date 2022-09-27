package eu.csgroup.coprs.monitoring.common.properties;

import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

public class PropertyUtil {

    private PropertyUtil () {
        
    }
    public static final String PROPERTY_DELIMITER = ".";

    public static final String ESCAPED_DELIMITER = "..";

    public static final String INDEX_START = "[";

    public static final String INDEX_END = "]";

    public static final String ATTRIBUTE_START = "[@";

    public static final String ATTRIBUTE_END = "]";


    /**
     * Surround property containing [] with [].
     * Example: giving log.trace.task.input[filename_strings][0] will result to [log.trace.task.input[filename_strings][0]]
     *
     * @param propertyName Property name to surround with []
     * @return surrounded property name with [] otherwise unchanged property name.
     */
    public static String surroundPropertyName (String propertyName) {
        if (propertyName.contains("[")) {
            return "[%s]".formatted(propertyName);
        } else {
            return propertyName;
        }
    }

    /**
     * Combine two path into one.
     * Example:
     * <ul>
     *     <li>rootPath: log.trace.task</li>
     *     <li>propertyName: input[filename_strings][0]</li>
     * </ul>
     * Will give the following result: log.trace.task.input[filename_strings][0]
     * <p>
     * If property name parameter start with a '[', property delimiter {@link PropertyUtil#PROPERTY_DELIMITER}
     * won't be used.
     * Example:
     * <ul>
     *     <li>rootPath: log.trace.task.input</li>
     *     <li>propertyName: [filename_strings][0]</li>
     * </ul>
     * Will give the following result: log.trace.task.input[filename_strings][0]
     *
     * @param rootPath start of the path
     * @param propertyName next part of the path
     * @return concatenated path
     */
    public static String getPath (String rootPath, String propertyName) {
        if (rootPath == null || rootPath.isEmpty()) {
            return propertyName;
        } else if (propertyName.startsWith(INDEX_START)) {
            return "%s%s".formatted(rootPath, propertyName);
        } else {
            return "%s%s%s".formatted(rootPath, PROPERTY_DELIMITER, propertyName);
        }
    }

    /**
     * Split path by using {@link PropertyUtil#PROPERTY_DELIMITER} as delimiter and limit the number
     * of part.
     * For example, applying a limit of 3 split will give the following result:
     * <ul>
     *     <li>log</li>
     *     <li>trace</li>
     *     <li>ask.input[filename_strings][0]</li>
     * </ul>
     *
     * @param path Path to split
     * @param limit Max number of path part
     * @return An array with splitted path
     */
    public static String[] splitPath (String path, int limit) {
        return path.split(Pattern.quote(PROPERTY_DELIMITER), limit);
    }

    /**
     * Split path by using {@link PropertyUtil#PROPERTY_DELIMITER} as delimiter
     * and then applying combination of '{@link  PropertyUtil#INDEX_START}{@link  PropertyUtil#INDEX_START}' as delimiter
     * on the first result.
     * Example: following path log.trace.task.input[filename_strings][0]
     * will give:
     * <ul>
     *   <li>log</li>
     *   <li>trace</li>
     *   <li>task</li>
     *   <li>input[filename_strings][0]</li>
     * </ul>
     * <p>
     * And then:
     * <ul>
     *   <li>log</li>
     *   <li>trace</li>
     *   <li>task</li>
     *   <li>input</li>
     *   <li>[filename_strings]</li>
     *   <li>[0]</li>
     * </ul>
     *
     * @param path The path to split
     * @return An array with splitted path
     */
    public static String[] splitPath (String path) {
        return Arrays.stream(path.split(Pattern.quote(PROPERTY_DELIMITER)))
                .map(PropertyUtil::splitIndex)
                .flatMap(Collection::stream)
                .toArray(String[]::new);
    }

    private static List<String> splitIndex (String path) {
        final var parts = new ArrayList<String>();
        Collections.addAll(
                parts,
                path.split("\\" + INDEX_END + "\\" + INDEX_START)
        );

        if (parts.size() != 1) {
            final var length = parts.size();

            // Add to each part ] that was removed by the first split
            // (except fot the last part that was not removed)
            // [product_metadata_custom_object => [product_metadata_custom_object]
            for (int index = 0; index < length - 1; index++) {
                parts.set(index, parts.get(index) + INDEX_END);
            }

            // Add to each part [ that was removed by the first split
            // (except for the first part that was not removed)
            // product_metadata_custom_object] => [product_metadata_custom_object]
            for (int index = 1; index < length; index++) {
                parts.set(index, INDEX_START + parts.get(index));
            }
        }

        // Split following path: missing_output[product_metadata_custom_object] into missing_output, [product_metadata_custom_object]
        final var tempParts = new ArrayList<>(parts);
        parts.clear();
        for (String part: tempParts) {
            if (part.contains(INDEX_START) && ! part.startsWith(INDEX_START)) {
                parts.add(part.substring(0, part.indexOf(INDEX_START)));
                parts.add(part.substring(part.indexOf(INDEX_START)));
            } else {
                parts.add(part);
            }
        }

        return parts;
    }

    public static String getParentPath(String path) {
        var bracketIndex = path.lastIndexOf(INDEX_START);
        var propertyDelimiterIndex = path.lastIndexOf(PROPERTY_DELIMITER);

        if (bracketIndex > propertyDelimiterIndex) {
            return path.substring(0, bracketIndex);
        } else if (bracketIndex < propertyDelimiterIndex){
            return path.substring(0, propertyDelimiterIndex);
        } else {
            return null;
        }
    }

    public static String snake2CamelCasePath (String snakePropertyPath) {
        return snake2CamelCasePath(snakePropertyPath, false);
    }

    public static String snake2CamelCasePath (String snakePropertyPath, boolean pascalCase) {
        return Arrays.stream(splitPath(snakePropertyPath))
                .map(prop -> PropertyUtil.snake2CamelCasePropertyName(prop, pascalCase))
                .reduce("", PropertyUtil::getPath);
    }

    public static String snake2CamelCasePropertyName (String snakePropertyName) {
        return snake2CamelCasePropertyName(snakePropertyName, false);
    }

    private static String snake2CamelCasePropertyName (String snakePropertyName, boolean pascalCase) {
        final var bracketIndex = snakePropertyName.indexOf(INDEX_START);
        var part2Camelise = snakePropertyName;
        var partNot2Camelise = "";
        if (bracketIndex != -1) {
            part2Camelise = snakePropertyName.substring(0,bracketIndex);
            partNot2Camelise = snakePropertyName.substring(bracketIndex);
        }

        int pos = 0;
        int index = 0;
        final var camelCasePropertyName = new StringBuilder();

        do {
            index = part2Camelise.indexOf("_", pos);
            if (index != -1) {
                var raw = part2Camelise.substring(pos, index);
                var newPart = raw;

                // Do not capitalize first part (beginning of property name), keep raw value as is
                if (pos != 0 || pascalCase) {
                    newPart = StringUtils.capitalize(raw);
                }
                camelCasePropertyName.append(newPart);

                pos = index + 1;
            }
        } while (index != -1);

        // Take into account end of the string
        if (pos != part2Camelise.length()) {
            var raw = part2Camelise.substring(pos);
            var endProp = raw;

            // No _ in property name so do not convert it and keep raw value as is.
            if (pos != 0 || pascalCase) {
                endProp = StringUtils.capitalize(raw);
            }
            camelCasePropertyName.append(endProp);
        }

        camelCasePropertyName.append(partNot2Camelise);

        return camelCasePropertyName.toString();
    }

    public static String snake2PascalCasePath (String snakePropertyPath) {
        return snake2CamelCasePath(snakePropertyPath, true);
    }

    public static String snake2PascalCasePropertyName (String snakePropertyName) {
        return snake2CamelCasePropertyName(snakePropertyName, true);
    }

    public static String pascal2SnakeCasePropertyName (String pascalPropertyName) {
        final var matcher = Pattern.compile("([A-Z]{1}[^A-Z]*)").matcher(pascalPropertyName);

        final var snakeCasePropertyName = new StringBuilder();
        while (matcher.find()) {
            final var group = matcher.group();

            if (! snakeCasePropertyName.isEmpty()) {
                snakeCasePropertyName.append("_");
            }
            snakeCasePropertyName.append(group.substring(0, 1).toLowerCase())
                    .append(group.substring(1));
        }

        if (snakeCasePropertyName.isEmpty()) {
            return pascalPropertyName;
        } else {
            return snakeCasePropertyName.toString();
        }
    }
}