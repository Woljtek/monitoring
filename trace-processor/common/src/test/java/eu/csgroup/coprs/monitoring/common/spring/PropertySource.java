package eu.csgroup.coprs.monitoring.common.spring;

import eu.csgroup.coprs.monitoring.common.properties.ReloadableYamlPropertySourceFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@org.springframework.context.annotation.PropertySource(name = "propertySourceTest", value = "${test.path}", factory = ReloadableYamlPropertySourceFactory.class)
@ConfigurationProperties
@Slf4j
public class PropertySource {
    List<String> collection;

    Map<String, String> map;

    int intValue;
}
