package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.jpa.EntityRepository;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.springframework.data.jpa.domain.Specification.where;

@Slf4j
@Configuration
@EnableJpaRepositories(basePackages = "eu.csgroup.coprs.monitoring.common.jpa")
@EntityScan(basePackages = EntityIngestor.BASE_PACKAGE)
public class EntityIngestor implements EntityFinder {
    public static final String BASE_PACKAGE = "eu.csgroup.coprs.monitoring.common.datamodel.entities";

    @Autowired
    private EntityRepository<ExternalInput> eiRepository;

    @Autowired
    private EntityRepository<Dsib> dRepository;

    @Autowired
    private EntityRepository<Chunk> cRepository;

    @Autowired
    private EntityRepository<AuxData> adRepository;

    @Autowired
    private EntityRepository<Product> pRepository;

    public List<? extends DefaultEntity> list() {
        return Stream.of(eiRepository, pRepository)
                .map(JpaRepository::findAll)
                .reduce(new Vector<>(), (l,n) -> {
                    l.addAll(n);
                    return l;
                });
    }

    public <T extends DefaultEntity> List<T> list(Class<T> className) {
        return selectRepository(className).findAll();
    }


    public <T extends DefaultEntity> EntityRepository selectRepository(Class<T> className) {
        EntityRepository repository = null;
        if (className.equals(AuxData.class)) {
            repository = adRepository;
        } else if (className.equals(Chunk.class)) {
            repository = cRepository;
        } else if (className.equals(Dsib.class)) {
            repository = dRepository;
        } else if (className.equals(Product.class)) {
            repository = pRepository;
        }
        else {
            repository = eiRepository;
        }

        return repository;
    }

    public <T extends DefaultEntity> List<T> saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        } else {
            return selectRepository(entities.get(0).getClass()).saveAll(entities);
        }
    }

    public DefaultEntity findEntityBy (Map<String, String> attributes) {
        final var clause = attributes.entrySet()
                .stream()
                .map(entry -> EntitySpecification.<ExternalInput>getEntityBy(entry.getKey(), entry.getValue()))
                .reduce(where(null), (l,n) -> l.and(n));

        return eiRepository.findOne(clause)
                .orElse(null);
    }

    public <T extends DefaultEntity> List<T> findAll(Class<T> className) {
        return selectRepository(className).findAll();
    }

    @Override
    public <T extends DefaultEntity> List<T> findAll(Specification<T> specs, Class<T> className) {
        return selectRepository(className).findAll(specs);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T extends DefaultEntity> List<T> process(Supplier<List<T>> processor) {
        return saveAll(processor.get());
    }

    /**
     * Do not use in production context. Must only be used for test context.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAll () {
        eiRepository.deleteAll();
        pRepository.deleteAll();
    }
}
