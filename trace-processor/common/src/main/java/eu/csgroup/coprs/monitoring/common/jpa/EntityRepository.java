package eu.csgroup.coprs.monitoring.common.jpa;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;

@NoRepositoryBean
public interface EntityRepository<T extends DefaultEntity, R> extends JpaRepository<T, R>, JpaSpecificationExecutor<T>  {

    @Override
    List<T> findAll(Specification<T> spec);

    @Override
    @Modifying
    @Query("DELETE FROM #{#entityName}")
    void deleteAll();
}
