package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

public interface EntityFinder {
    <T extends DefaultEntity> List<T> findAll(Specification<T> specs, Class<T> className);
}
