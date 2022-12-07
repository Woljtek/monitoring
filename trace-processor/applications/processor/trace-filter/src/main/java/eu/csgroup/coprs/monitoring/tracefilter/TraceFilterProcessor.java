package eu.csgroup.coprs.monitoring.tracefilter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
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
            final var traceLog = jsonValidator.readAndValidate(rawJson, TraceLog.class);

            // Create wrapper to access value with path
            final var beanAccessor = BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(traceLog));
            beanAccessor.getDelegate().setAutoGrowNestedPaths(true);

            // Prepare log result
            var traceLogDebug = new StringBuilder("Trace handled");

            var matchingFilter = Optional.of(beanAccessor).flatMap(factory.getBean(FilterGroup.class));

            // Finalize log result and send it
            matchingFilter.ifPresentOrElse(filterName -> traceLogDebug.append("filter '").append(filterName).append("' applied"),
                () -> traceLogDebug.append("no filter applied")
            );
            log.debug(traceLogDebug.append("(").append(rawJson).append(")").toString());

            // Reset cached error trace
            lastProcessedRawTrace = null;

            // Create message
            return matchingFilter.map(filter -> new FilteredTrace(filter.getName(), traceLog))
                .map(ft -> MessageBuilder.withPayload(ft).build())
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
        } catch (JsonProcessingException | JsonValidationException e) {
            log.error("Wrong trace format (%s)".formatted(rawJson), e);
            return Collections.emptyList();
        } catch (Exception e) {
            lastProcessedRawTrace = rawJson;
            final var errorMessage = "Error occurred handling trace %n%s: ".formatted(rawJson);
            throw new FilterException(errorMessage, e);
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
        return dirtyJson.replaceAll("\\\\*\"", "\"")
                .replace("\"{", "{")
                .replace("}\"", "}");
    }
}
