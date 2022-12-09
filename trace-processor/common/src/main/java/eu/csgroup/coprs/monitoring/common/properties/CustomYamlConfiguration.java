package eu.csgroup.coprs.monitoring.common.properties;

import lombok.Getter;
import org.apache.commons.configuration2.YAMLConfiguration;

import java.util.Map;

/**
 * Workaround class to access to raw configuration map because {@link YAMLConfiguration} given access to a tree
 * associated to the configuration file without indicating if it's represented as a list in the configuration file
 * (needed when spring boot create configuration file where path must contain '[' and ']' to represent a list object)
 */
public class CustomYamlConfiguration extends YAMLConfiguration {
    /**
     * Raw structure of configuration file
     */
    @Getter
    private Map<String, Object> cache;


    @Override
    protected void load(Map<String, Object> map) {
        this.cache = map;
        super.load(map);
    }
}