package eu.csgroup.coprs.monitoring.common.properties;

import lombok.Data;
import org.apache.commons.configuration2.YAMLConfiguration;

import java.util.Map;

@Data
public class CustomYamlConfiguration extends YAMLConfiguration {
    private Map<String, Object> cache;

    @Override
    protected void load(Map<String, Object> map) {
        this.cache = map;
        super.load(map);
    }
}
