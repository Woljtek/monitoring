package eu.csgroup.coprs.monitoring.common.spring;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityMetadata;
import eu.csgroup.coprs.monitoring.common.ingestor.RepositoryNotFoundException;
import eu.csgroup.coprs.monitoring.common.jpa.EntityRepository;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.common.jpa.ExternalInputRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@Import(EntityIngestor.class)
@DataJpaTest
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("dev-embedded")
public class EntityIngestorTest {

    @Autowired
    private EntityIngestor entityIngestor;

    @Test
    public void testSave() {
        // Given
        final var dsib = new Dsib();
        dsib.setFilename("test");
        final var dsib2 = new Dsib();
        dsib2.setFilename("res");

        // When
        final var emptyRes = entityIngestor.saveAll(List.of());
        final var res = entityIngestor.saveAll(List.of(dsib));
        final var res2 = entityIngestor.process((e) -> List.of(dsib2));

        // Then
        assertThat(emptyRes).isEmpty();
        assertThat(res).hasSize(1).allMatch(entity -> ((ExternalInput)entity).getId() != null);
        assertThat(res2).hasSize(1).allMatch(entity -> ((ExternalInput)entity).getId() != null);
    }


    @Test
    public void testFindAll() {
        // Given
        final var dsib = new Dsib();
        dsib.setFilename("test");
        final var dsib2 = new Dsib();
        dsib2.setFilename("res");
        entityIngestor.saveAll(List.of(dsib, dsib2));

        // When
        final var allRes = entityIngestor.findAll(Dsib.class);
        final var entityByRes = entityIngestor.findEntityBy(Dsib.class, Map.of("filename", "test"));
        final var res = entityIngestor.findAll(Specification.where(EntitySpecification.getEntityBy("filename", "test")), Dsib.class);

        // Then
        assertThat(allRes).hasSize(2).allMatch(entity -> entity.getId() != null);
        assertThat(entityByRes)
                .isNotEmpty()
                .hasSize(1)
                .allMatch(entity -> entity.getFilename().equals("test"))
                .isEqualTo(res);
    }

    @Test
    public void testOrderEntity () {
        // Given
        final var dsib = new Dsib();
        dsib.setFilename("dsib");
        final var chunk = new Chunk();
        chunk.setFilename("chunk");
        final var auxData = new AuxData();
        auxData.setFilename("auxData");
        final var processing = new Processing();
        final var inputListExternal = new InputListExternal();
        inputListExternal.getId().setProcessing(processing);
        inputListExternal.getId().setExternalInput(dsib);

        final var product = new Product();
        final var inputListInternal = new InputListInternal();
        inputListInternal.getId().setProcessing(processing);
        inputListInternal.getId().setProduct(product);

        final var outputList = new OutputList();
        outputList.getId().setProcessing(processing);
        outputList.getId().setProduct(product);

        final var missingProduct = new MissingProducts();
        missingProduct.setProcessing(processing);

        // When
        final var list = new LinkedList<DefaultEntity>();
        list.add(dsib);
        list.add(inputListExternal);
        list.add(inputListInternal);
        list.add(processing);
        list.add(chunk);
        list.add(outputList);
        list.add(missingProduct);
        list.add(product);
        list.add(auxData);
        final var res = entityIngestor.saveAll(list);

        // Then
        assertThat(res).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetRepository () {
        // Given
        final var externalInputCLass = ExternalInput.class;
        final var defaultEntityCLass = DefaultEntity.class;

        // When
        final var externalInputRespository = entityIngestor.selectRepository(externalInputCLass);
        final var unknownRepository = assertThatThrownBy(() -> entityIngestor.selectRepository(defaultEntityCLass));

        // Then
        assertThat(externalInputRespository)
                .isNotNull()
                .matches(ExternalInputRepository.class::isInstance);

        unknownRepository
                .isNotNull()
                .isInstanceOf(RepositoryNotFoundException.class);
    }
}
