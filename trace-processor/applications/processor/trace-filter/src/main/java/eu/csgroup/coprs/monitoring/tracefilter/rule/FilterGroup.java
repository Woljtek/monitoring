package eu.csgroup.coprs.monitoring.tracefilter.rule;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.properties.ReloadableYamlPropertySourceFactory;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanWrapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
@Configuration
@PropertySource(name = "filterGroup", value = "${filter.path}", factory = ReloadableYamlPropertySourceFactory.class)
@ConfigurationProperties()
@Slf4j
public class FilterGroup implements Function<BeanAccessor, Optional<Filter>> {
    List<Filter> filters;

    @Override
    public Optional<Filter> apply(BeanAccessor node) {
        final Predicate<Filter> checkFilter = filter -> filter.test(node);

        return filters
                .stream()
                .peek(filter -> log.trace("Apply filter %s".formatted(filter.getName())))
                .filter(checkFilter)
                .findFirst();
    }
}
