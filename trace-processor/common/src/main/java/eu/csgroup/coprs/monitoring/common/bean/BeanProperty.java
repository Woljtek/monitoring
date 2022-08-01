package eu.csgroup.coprs.monitoring.common.bean;

import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BeanProperty {
    /**
     * Snake case
     */
    @ToString.Include
    @EqualsAndHashCode.Include
    private final String rawPropertyPath;
    private String rawBeanPropertyPath;

    /**
     * Pascal case
     */
    private String beanName;

    /**
     * Camel case
     */
    private String beanPropertyPath;

    /**
     * Snake case
     */
    private String propertyName;


    public BeanProperty (String property) {
        this.rawPropertyPath = property;
        convert();
    }

    private void convert () {
        final var splittedPath = PropertyUtil.splitPath(rawPropertyPath, 2);
        beanName = PropertyUtil.snake2PascalCasePropertyName(splittedPath[0]);
        rawBeanPropertyPath = splittedPath[1];
        beanPropertyPath = PropertyUtil.snake2CamelCasePath(rawBeanPropertyPath);

        propertyName = rawBeanPropertyPath;
        while (propertyName.contains(PropertyUtil.INDEX_START)) {
            propertyName = propertyName.substring(
                    propertyName.indexOf(PropertyUtil.INDEX_START) + 1,
                    propertyName.indexOf(PropertyUtil.INDEX_END)
            );
        }

        int lastDelimiter = propertyName.lastIndexOf(PropertyUtil.PROPERTY_DELIMITER);
        if (lastDelimiter != -1) {
            propertyName = propertyName.substring(lastDelimiter + 1);
        }
    }
}
