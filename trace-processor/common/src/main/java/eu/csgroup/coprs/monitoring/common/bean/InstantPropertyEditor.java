package eu.csgroup.coprs.monitoring.common.bean;

import com.fasterxml.jackson.databind.node.TextNode;
import eu.csgroup.coprs.monitoring.common.json.PropertyNames;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InstantPropertyEditor implements PropertyEditor {
    private Instant instant;

    private DateTimeFormatter formatter;


    public InstantPropertyEditor () {
        this(PropertyNames.DATE_PATTERN, PropertyNames.DEFAULT_TIMEZONE);
    }

    public InstantPropertyEditor (String datePattern, String timeZone) {
        formatter = DateTimeFormatter.ofPattern(datePattern)
                .withZone(ZoneId.of(timeZone));
    }

    @Override
    public void setValue(Object value) {
        if (value == null) {
            instant = null;
        } else if (value instanceof Instant) {
            instant = (Instant) value;
        } else {
            String rawDate = null;
            if (value instanceof TextNode) {
                rawDate = ((TextNode) value).asText();
            } else {
                rawDate = value.toString();
            }
            instant = Instant.parse(rawDate.toString());
        }
    }

    @Override
    public Object getValue() {
        return instant;
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {

    }

    @Override
    public String getJavaInitializationString() {
        return null;
    }

    @Override
    public String getAsText() {
        return formatter.format(instant);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        instant = Instant.parse(text);
    }

    @Override
    public String[] getTags() {
        return new String[0];
    }

    @Override
    public Component getCustomEditor() {
        return null;
    }

    @Override
    public boolean supportsCustomEditor() {
        return false;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }
}
