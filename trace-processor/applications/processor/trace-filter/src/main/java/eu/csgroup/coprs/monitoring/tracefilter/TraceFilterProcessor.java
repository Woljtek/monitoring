package eu.csgroup.coprs.monitoring.tracefilter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.TraceLog;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import eu.csgroup.coprs.monitoring.tracefilter.json.JsonValidationException;
import eu.csgroup.coprs.monitoring.tracefilter.json.JsonValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.csgroup.coprs.monitoring.tracefilter.rule.FilterGroup;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraceFilterProcessor
    implements Function<Message<String>, List<Message<FilteredTrace>>> {

    private final JsonValidator jsonValidator;

    private final ReloadableBeanFactory factory;

    private String lastProcessedRawTrace;

    public TraceFilterProcessor(JsonValidator jsonValidator, ReloadableBeanFactory factory) {
        this.jsonValidator = jsonValidator;
        this.factory= factory;
    }

    public List<Message<FilteredTrace>> apply(Message<String> json) {
        final var rawJson = undecorate(json.getPayload());
        try {
            if (lastProcessedRawTrace != null && lastProcessedRawTrace.equals(rawJson)) {
                log.trace("Retry last failed trace");
            } else {
                log.trace("Handle new trace");
            }

            // Map json to bean anc check if json is compliant to ICD
            final var trace = jsonValidator.readAndValidate(rawJson, TraceLog.class).getTrace();

            // Create wrapper to access value with path
            final var beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(trace);
            beanWrapper.setAutoGrowNestedPaths(true);

            // Prepare log result
            var traceLog = new StringBuilder("Trace handled");

            var matchingFilter = Optional.of(beanWrapper).flatMap(factory.getBean(FilterGroup.class));

            // Finalize log result and send it
            matchingFilter.ifPresentOrElse(filterName -> traceLog.append("filter '").append(filterName).append("' applied"),
                () -> traceLog.append("no filter applied")
            );
            log.debug(traceLog.append("(").append(rawJson).append(")").toString());

            // Reset cached error trace
            lastProcessedRawTrace = null;

            // Create message
            return matchingFilter.map(filter -> new FilteredTrace(filter.getName(), trace))
                .map(ft -> MessageBuilder.withPayload(ft).build())
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
        } catch (JsonProcessingException | JsonValidationException e) {
            log.error("Wrong trace format (%s)".formatted(rawJson), e);
            return Collections.emptyList();
        } catch (Exception e) {
            lastProcessedRawTrace = rawJson;
            final var errorMessage = "Error occurred handling trace \n%s: ".formatted(rawJson);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Update structure to be compliant with JSON SPEC.
     * Replace following structure with:
     * <ul>
     *     <li>\" => "</li>
     *     <li>"{ => {</li>
     *     <li>}" => }</li>
     * </ul>
     *
     * @param dirtyJson Non-compliant structure to JSON SPEC
     * @return Updated structure
     */
    public String undecorate (String dirtyJson) {
        return dirtyJson.replaceAll("\\\\\"", "\"")
                .replaceAll("\"\\{", "{")
                .replaceAll("}\"", "}");
    }
}
