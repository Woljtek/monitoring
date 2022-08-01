package eu.csgroup.coprs.monitoring.common.properties;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

public class PropertyUtil {
    public static final String PROPERTY_DELIMITER = ".";

    public static final String ESCAPED_DELIMITER = "..";

    public static final String INDEX_START = "[";

    public static final String INDEX_END = "]";

    public static final String ATTRIBUTE_START = "[@";

    public static final String ATTRIBUTE_END = "]";



    public static String surroundPropertyName (String propertyName) {
        if (propertyName.contains("[")) {
            return "[%s]".formatted(propertyName);
        } else {
            return propertyName;
        }
    }

    public static String getPath (String rootPath, String propertyName) {
        if (rootPath == null || rootPath.isEmpty()) {
            return propertyName;
        } else {
            return "%s%s%s".formatted(rootPath, PROPERTY_DELIMITER, propertyName);
        }
    }

    public static String[] splitPath (String path, int limit) {
        return path.split(Pattern.quote(PROPERTY_DELIMITER), limit);
    }

    public static String[] splitPath (String path) {
        return path.split(Pattern.quote(PROPERTY_DELIMITER));
    }

    /**
     * Remove snake and camel case formatting (set property in lower case).
     *
     * @param formattedPropertyName
     * @return
     */
    public static String removeAllFormat (String formattedPropertyName) {
        return formattedPropertyName.replaceAll("_", "").toLowerCase();
    }

    public static String snake2CamelCasePath (String snakePropertyPath) {
        return Arrays.stream(splitPath(snakePropertyPath))
                .map(prop -> PropertyUtil.snake2CamelCasePropertyName(prop, false))
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
            partNot2Camelise = snakePropertyName.substring(bracketIndex, snakePropertyName.length());
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

    public static String snake2PascalCasePropertyPath (String snakePropertyPath) {
        return snake2CamelCasePath(snakePropertyPath);
    }

    public static String snake2PascalCasePropertyName (String snakePropertyName) {
        return snake2CamelCasePropertyName(snakePropertyName, true);
    }

    public static String pascal2SnakeCasePropertyName (String pascalPropertyName) {
        final var array = pascalPropertyName.split("[A-Z]");
        StringBuilder snakePropertyNameBuilder = new StringBuilder(array[0].toLowerCase());
        int index = 1;

        while (index < array.length) {
            snakePropertyNameBuilder.append("_");
            snakePropertyNameBuilder.append(array[index]);
        }

        return snakePropertyNameBuilder.toString();
    }
}
