package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.jpa.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.jpa.domain.Specification.where;

@Slf4j
@Configuration
@EnableJpaRepositories(basePackages = "eu.csgroup.coprs.monitoring.common.jpa")
@EntityScan(basePackages = EntityIngestor.BASE_PACKAGE)
public class EntityIngestor implements EntityFinder {
    public static final String BASE_PACKAGE = "eu.csgroup.coprs.monitoring.common.datamodel.entities";

    @Autowired
    private ExternalInputRepository eiRepository;

    @Autowired
    private DsibRepository dRepository;

    @Autowired
    private ChunkRepository cRepository;

    @Autowired
    private AuxDataRepository adRepository;

    @Autowired
    private ProductRepository prodRepository;

    @Autowired
    private ProcessingRepository procRepository;

    @Autowired
    private InputListExternalRepository ileRepository;

    @Autowired
    private InputListInternalRepository iliRepository;

    @Autowired
    private OutputListRepository olRepository;

    @Autowired
    private MissingProductsRepository mpRepository;

    public List<? extends DefaultEntity> list() {
        return Stream.of(eiRepository, prodRepository)
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
        } else if (className.equals(ExternalInput.class)) {
            repository = eiRepository;
        } else if (className.equals(Product.class)) {
            repository = prodRepository;
        } else if (className.equals(Processing.class)) {
            repository = procRepository;
        } else if (className.equals(InputListExternal.class)) {
            repository = ileRepository;
        } else if (className.equals(InputListInternal.class)) {
            repository = iliRepository;
        } else if (className.equals(OutputList.class)) {
            repository = olRepository;
        } else if (className.equals(MissingProducts.class)) {
            repository = mpRepository;
        }
        else {
            // TODO
            throw new RuntimeException("Repository for entity %s not found".formatted(className.getName()));
        }

        return repository;
    }

    public List<DefaultEntity> saveAll(List<DefaultEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        } else {
            final var comparator = new EntityComparator();
            final var groupedEntity = entities.stream()
                    .collect(Collectors.groupingBy(entity -> entity.getClass()));

            final var order = new LinkedList<Class>();
            groupedEntity.keySet()
                    .stream()
                    .map(entityClass -> EntityFactory.getInstance().getMetadata(entityClass))
                    .forEach(entityMetadata -> {
                        log.debug("Check order for entity %s".formatted(entityMetadata.getEntityName()));
                        if (entityMetadata.getRelyOn().isEmpty()) {
                            order.addFirst(entityMetadata.getEntityClass());
                        } else {
                            final var indexRelyOn = EntityHelper.getDeepRelyOn(entityMetadata).map(order::indexOf)
                                    .reduce(-1, (l,n) -> l > n ? l : n);
                            log.debug("RelyOn order: %s".formatted(indexRelyOn));
                            final var indexReferencedBy = EntityHelper.getDeepReferencedBy(entityMetadata).map(order::indexOf)
                                    .filter(ReferencedByIndex -> ReferencedByIndex != -1)
                                    .reduce(-1, (l,n) -> l > n ? l : n);
                            log.debug("ReferencedBy order : %s".formatted(indexReferencedBy));
                            var index = indexRelyOn > indexReferencedBy ? indexRelyOn : indexReferencedBy;
                            index++;

                            log.debug("Order of entity %s: %s".formatted(entityMetadata.getEntityName(), index));
                            order.add(index, entityMetadata.getEntityClass());
                        }
                        log.debug("Order: %s".formatted(order));
                    });
            return order.stream()
                    .peek(entityClass -> log.debug("Save entity %s".formatted(entityClass.getSimpleName())))
                    .map(entityClass -> Map.entry(entityClass, groupedEntity.get(entityClass)))
                    .flatMap(entry -> selectRepository(entry.getKey()).saveAll(entry.getValue()).stream())
                    .toList();
        }
    }



    public DefaultEntity findEntityBy (Map<String, String> attributes) {
        final var clause = attributes.entrySet()
                .stream()
                .map(entry -> EntitySpecification.<ExternalInput>getEntityBy(entry.getKey(), entry.getValue()))
                .reduce(where(null), Specification::and);

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
    public List<DefaultEntity> process(Function<EntityIngestor, List<DefaultEntity>> processor) {
        return saveAll(processor.apply(this));
    }

    /**
     * Do not use in production context. Must only be used for test context.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAll () {
        ileRepository.deleteAll();
        iliRepository.deleteAll();
        olRepository.deleteAll();
        cRepository.deleteAll();
        eiRepository.deleteAll();
        prodRepository.deleteAll();
        mpRepository.deleteAll();
        procRepository.deleteAll();
    }
}
