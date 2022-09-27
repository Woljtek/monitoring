package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.jpa.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    public <T extends DefaultEntity> List<T> list(Class<T> className) {
        return selectRepository(className).findAll();
    }


    public <T extends DefaultEntity, E> EntityRepository<T,E> selectRepository(Class<T> className) {
        if (className.equals(AuxData.class)) {
            return castToGenericRepository(adRepository);
        } else if (className.equals(Chunk.class)) {
            return castToGenericRepository(cRepository);
        } else if (className.equals(Dsib.class)) {
            return castToGenericRepository(dRepository);
        } else if (className.equals(ExternalInput.class)) {
            return castToGenericRepository(eiRepository);
        } else if (className.equals(Product.class)) {
            return castToGenericRepository(prodRepository);
        } else if (className.equals(Processing.class)) {
            return castToGenericRepository(procRepository);
        } else if (className.equals(InputListExternal.class)) {
            return castToGenericRepository(ileRepository);
        } else if (className.equals(InputListInternal.class)) {
            return castToGenericRepository(iliRepository);
        } else if (className.equals(OutputList.class)) {
            return castToGenericRepository(olRepository);
        } else if (className.equals(MissingProducts.class)) {
            return castToGenericRepository(mpRepository);
        } else {
            throw new RepositoryNotFoundException("Repository for entity %s not found".formatted(className.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends DefaultEntity, E> EntityRepository<T,E> castToGenericRepository (
            EntityRepository<? extends DefaultEntity,? extends Serializable> specializedRepository) {
        return (EntityRepository<T,E>) specializedRepository;
    }

    public List<DefaultEntity> saveAll(List<DefaultEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        } else {
            final var groupedEntity = entities.stream()
                    .collect(Collectors.groupingBy(DefaultEntity::getClass));

            final var order = new LinkedList<Class<DefaultEntity>>();
            groupedEntity.keySet()
                    .stream()
                    .map(entityClass -> EntityFactory.getInstance().getMetadata(entityClass))
                    .forEach(entityMetadata -> orderEntityType(order, entityMetadata));
            return order.stream()
                    .map(entityClass -> {
                        log.debug("Save entity %s".formatted(entityClass.getSimpleName()));
                        return entityClass;
                    })
                    .map(entityClass -> Map.entry(entityClass, groupedEntity.get(entityClass)))
                    .flatMap(entry -> selectRepository(entry.getKey()).saveAll(entry.getValue()).stream())
                    .toList();
        }
    }

    private void orderEntityType (LinkedList<Class<DefaultEntity>> orderedEntityType, EntityMetadata entityMetadata) {
        log.debug("Check order for entity %s".formatted(entityMetadata.getEntityName()));
        if (entityMetadata.getRelyOn().isEmpty()) {
            orderedEntityType.addFirst(entityMetadata.getEntityClass());
        } else {
            final var indexRelyOn = EntityHelper.getDeepRelyOn(entityMetadata).map(orderedEntityType::indexOf)
                    .reduce(-1, (l,n) -> l > n ? l : n);
            log.debug("RelyOn order: %s".formatted(indexRelyOn));
            final var indexReferencedBy = EntityHelper.getDeepReferencedBy(entityMetadata).map(orderedEntityType::indexOf)
                    .filter(referencedByIndex -> referencedByIndex != -1)
                    .reduce(-1, (l,n) -> l > n ? l : n);
            log.debug("ReferencedBy order : %s".formatted(indexReferencedBy));
            var index = indexRelyOn > indexReferencedBy ? indexRelyOn : indexReferencedBy;
            index++;

            log.debug("Order of entity %s: %s".formatted(entityMetadata.getEntityName(), index));
            orderedEntityType.add(index, entityMetadata.getEntityClass());
        }
        log.debug("Order: %s".formatted(orderedEntityType));
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
