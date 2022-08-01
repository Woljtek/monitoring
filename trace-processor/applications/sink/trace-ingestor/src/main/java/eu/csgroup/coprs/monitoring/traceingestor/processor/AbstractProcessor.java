package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Data
public abstract class AbstractProcessor<T, R> implements Function<T, List<R>> {
    protected final String entityName;

    protected final List<Mapping> mappings;

    protected final List<BeanProperty> dependencies;

    protected final BiFunction<Specification<? extends ExternalInput>, Class<? extends ExternalInput>, List<? extends ExternalInput>> entityFinder;
}
