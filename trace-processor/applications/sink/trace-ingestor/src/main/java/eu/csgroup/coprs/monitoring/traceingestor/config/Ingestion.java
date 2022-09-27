package eu.csgroup.coprs.monitoring.traceingestor.config;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ingestion {
    private String name;
    private List<Mapping> mappings;
    private List<BeanProperty> dependencies = new ArrayList<>();
    private Map<String, Alias> alias = new HashMap<>();

    public void setAlias(Map<String, Alias> associations) {
        this.alias = associations.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> PropertyUtil.snake2PascalCasePropertyName(entry.getKey()),
                        Map.Entry::getValue)
                );
    }
}
