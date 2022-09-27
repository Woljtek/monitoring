package eu.csgroup.coprs.monitoring.common;

import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyUtilTest {

    @Test
    public void testSurroundPropertyName () {
        // Given
        final var propertyName = "log.trace.task.input[filename_strings][0]";
        final var propertyName2 = "log.trace.task.input.filename_strings.0";

        // When
        final var surroundedPropertyName = PropertyUtil.surroundPropertyName(propertyName);
        final var surroundedPropertyName2 = PropertyUtil.surroundPropertyName(propertyName2);

        // Then
        assertThat(surroundedPropertyName).isEqualTo("[log.trace.task.input[filename_strings][0]]");
        assertThat(surroundedPropertyName2).isEqualTo(propertyName2);
    }

    @Test
    public void testGetPath () {
        // Given
        final var rootPath = "log.trace.task";
        final var nextPath = "input[filename_strings][0]";
        final var completePath = "log.trace.task.input[filename_strings][0]";

        final var rootPath2 = "log.trace.task.input";
        final var nextPath2 = "[filename_strings][0]";

        // When
        final var concatenatedPath = PropertyUtil.getPath(rootPath, nextPath);
        final var concatenatedPath2 = PropertyUtil.getPath(rootPath2, nextPath2);

        // Then
        assertThat(concatenatedPath).isEqualTo(concatenatedPath2).isEqualTo(completePath);
    }

    @Test
    public void testSplittPath () {
        // Given
        final var completePath = "log.trace.task.input[filename_strings][0]";

        // When
        final var splittedPath = PropertyUtil.splitPath(completePath);
        final var limitedSplittedPath = PropertyUtil.splitPath(completePath, 3);

        assertThat(splittedPath).hasSize(6).containsExactly("log", "trace", "task", "input", "[filename_strings]", "[0]");
        assertThat(limitedSplittedPath).hasSize(3).containsExactly("log", "trace", "task.input[filename_strings][0]");
    }

    @Test
    public void testGetParentPath () {
        // Given
        final var completePath = "log.trace.task.input[filename_strings][0]";

        // When
        final var parentPath = PropertyUtil.getParentPath(completePath);
        final var parentPath2 = PropertyUtil.getParentPath(parentPath);
        final var parentPath3 = PropertyUtil.getParentPath(parentPath2);

        // Then
        assertThat(parentPath).isEqualTo("log.trace.task.input[filename_strings]");
        assertThat(parentPath2).isEqualTo("log.trace.task.input");
        assertThat(parentPath3).isEqualTo("log.trace.task");
    }

    @Test
    public void testSnake2CamelCase () {
        // Given
        final var completePath = "log.trace.task.missing_products[filename_strings][0]";
        final var propertyName = "missing_products[filename_strings][0]";

        // When
        final var camelCasePath = PropertyUtil.snake2CamelCasePath(completePath);
        final var camelCasePropertyName = PropertyUtil.snake2CamelCasePropertyName(propertyName);

        // Then
        assertThat(camelCasePath).isEqualTo("log.trace.task.missingProducts[filename_strings][0]");
        assertThat(camelCasePropertyName).isEqualTo("missingProducts[filename_strings][0]");
    }

    @Test
    public void testSnake2PascalCase () {
        // Given
        final var completePath = "log.trace.task.missing_products[filename_strings][0]";
        final var propertyName = "missing_products[filename_strings][0]";

        // When
        final var pascalCasePath = PropertyUtil.snake2PascalCasePath(completePath);
        final var pascalCasePropertyName = PropertyUtil.snake2PascalCasePropertyName(propertyName);

        // Then
        assertThat(pascalCasePath).isEqualTo("Log.Trace.Task.MissingProducts[filename_strings][0]");
        assertThat(pascalCasePropertyName).isEqualTo("MissingProducts[filename_strings][0]");
    }

    @Test
    public void testPascal2SnakeCase () {
        // Given
        final var propertyName = "MissingProducts[filename_strings][0]";
        final var propertyName2 = "missing_products[filename_strings][0]";

        // When
        final var snakeCasePropertyName = PropertyUtil.pascal2SnakeCasePropertyName(propertyName);
        final var snakeCasePropertyName2 = PropertyUtil.pascal2SnakeCasePropertyName(propertyName2);

        // Then
        assertThat(snakeCasePropertyName).isEqualTo("missing_products[filename_strings][0]");
        assertThat(snakeCasePropertyName2).isEqualTo("missing_products[filename_strings][0]");
    }
}
