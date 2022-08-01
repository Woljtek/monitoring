package eu.csgroup.coprs.monitoring.common.jpa;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface EntityRepository<T extends ExternalInput> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T>  {
    //@Query("select e from #{#entityName} e where e.?#{[0]} = ?#{[1]}")
    //public Iterable<T> findAll(String fieldName, String fieldValue);

    //@Override
    //Optional<T> findOne(Specification<T> spec);

    @Override
    List<T> findAll(Specification<T> spec);
}
