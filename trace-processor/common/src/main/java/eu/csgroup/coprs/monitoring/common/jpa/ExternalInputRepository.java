package eu.csgroup.coprs.monitoring.common.jpa;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

//@Transactional
@Repository
public interface ExternalInputRepository extends EntityRepository<ExternalInput> {
}
