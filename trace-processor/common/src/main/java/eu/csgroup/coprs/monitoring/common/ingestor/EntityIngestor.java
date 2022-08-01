package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.AuxData;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.common.jpa.EntityRepository;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.data.jpa.domain.Specification.where;

@Configuration
@EnableJpaRepositories(basePackages = "eu.csgroup.coprs.monitoring.common.jpa")
@EntityScan(basePackages = EntityIngestor.BASE_PACKAGE)
//@EnableTransactionManagement
//@Transactional
public class EntityIngestor {
    public static final String BASE_PACKAGE = "eu.csgroup.coprs.monitoring.common.datamodel.entities";

    @Autowired
    private EntityRepository<ExternalInput> eiRepository;

    @Autowired
    private EntityRepository<Dsib> dRepository;

    @Autowired
    private EntityRepository<Chunk> cRepository;

    @Autowired
    private EntityRepository<AuxData> adRepository;

    public Iterable<ExternalInput> list() {
        return eiRepository.findAll();
    }


    public EntityRepository selectRepository(Class<? extends ExternalInput> className) {
        EntityRepository repository = null;
        if (className.equals(AuxData.class)) {
            repository = adRepository;
        }
        else if (className.equals(Chunk.class)) {
            repository = cRepository;
        }
        else if (className.equals(Dsib.class)) {
            repository = dRepository;
        }
        else {
            repository = eiRepository;
        }

        return repository;
    }

    public List<ExternalInput> saveAll(List<ExternalInput> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        } else {
            return selectRepository(entities.get(0).getClass()).saveAll(entities);
        }
    }

    public ExternalInput findEntityBy (Map<String, String> attributes) {
        final var clause = attributes.entrySet()
                .stream()
                .map(entry -> EntitySpecification.getEntityBy(entry.getKey(), entry.getValue()))
                .reduce(where(null), (l,n) -> l.and(n));

        return eiRepository.findOne(clause)
                .orElse(null);
    }

    public <T extends ExternalInput> List<? extends ExternalInput> findAll(Specification<? extends ExternalInput> specs, Class<? extends ExternalInput> className) {
        return selectRepository(className).findAll(specs);
    }

    public List<ExternalInput> findAll(Class<? extends ExternalInput> className) {
        return selectRepository(className).findAll();
    }

}
