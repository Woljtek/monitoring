package eu.csgroup.coprs.monitoring.tracefilter;


import java.util.List;
import java.util.function.Function;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import eu.csgroup.coprs.monitoring.tracefilter.json.JsonValidator;
import eu.csgroup.coprs.monitoring.tracefilter.rule.FilterGroup;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

@Configuration
@EnableConfigurationProperties({TraceFilterProperties.class, FilterGroup.class})
@Import({JsonValidator.class, ReloadableBeanFactory.class})
public class TraceFilterConfiguration {

    @Bean
    public ObjectMapper traceMapper () {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Bean(name = "trace-filter")
    public Function<Message<String>, List<Message<FilteredTrace>>> traceFilter(JsonValidator jsonMapper, ReloadableBeanFactory factory) {
       return new TraceFilterProcessor(jsonMapper, factory);
    }

}
