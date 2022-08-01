package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

@Data
public abstract class AbstractProcessor<T, R> implements Function<T, List<R>> {
    protected final String entityName;

    protected final String configurationName;

    protected final List<Mapping> mappings;

    protected final List<BeanProperty> dependencies;

    protected final EntityFinder entityFinder;
}
