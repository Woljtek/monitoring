package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class TreePropertyLeaf extends TreeProperty {
    private final Mapping rule;

    private final Object rawValue;
    public TreePropertyLeaf(String path, Mapping rule, Object rawValue) {
        super(path);
        this.rule = rule;
        this.rawValue = rawValue;
    }
}
