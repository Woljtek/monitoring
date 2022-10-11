package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@EqualsAndHashCode()
public class TreePropertyLeaf implements TreeProperty {
    private final Mapping rule;

    private final Map<BeanProperty,Object> rawValues = new HashMap<>();

    public TreePropertyLeaf(Mapping rule) {
        this.rule = rule;
    }

    public void putRawValue (BeanProperty beanProperty, Object rawValue) {
        rawValues.put(beanProperty, rawValue);
    }

    public TreePropertyLeaf copy () {
        final var newLeaf = new TreePropertyLeaf(this.rule);

        newLeaf.rawValues.putAll(this.rawValues);

        return newLeaf;
    }
}
