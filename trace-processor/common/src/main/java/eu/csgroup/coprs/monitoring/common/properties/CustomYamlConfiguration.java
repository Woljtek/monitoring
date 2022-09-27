package eu.csgroup.coprs.monitoring.common.properties;

import lombok.Getter;
import org.apache.commons.configuration2.YAMLConfiguration;

import java.util.Map;

public class CustomYamlConfiguration extends YAMLConfiguration {
    @Getter
    private Map<String, Object> cache;


    @Override
    protected void load(Map<String, Object> map) {
        this.cache = map;
        super.load(map);
    }
}
