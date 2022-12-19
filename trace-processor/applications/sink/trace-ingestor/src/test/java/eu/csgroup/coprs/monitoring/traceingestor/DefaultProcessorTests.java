package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFactory;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.processor.DefaultProcessor;
import eu.csgroup.coprs.monitoring.traceingestor.processor.ProcessorDescription;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(TraceIngestorConfiguration.class)
@ContextConfiguration(initializers = TestInitializer.class)
@DataJpaTest
// Comment the two below annotation to test with non embedded database
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("dev-embedded")
//@ActiveProfiles("dev-integration")
public class DefaultProcessorTests {

    private static final TraceIngestorConfiguration conf = new TraceIngestorConfiguration();

    @Autowired
    private ReloadableBeanFactory factory;

    @Autowired
    private EntityIngestor entityIngestor;


    @After
    public void setUpAfterTest () {
        entityIngestor.deleteAll();
    }

    @Test
    public void testDeepLeafExtract () {
        // Given
        final var dsib1 = new Dsib();
        dsib1.setFilename("L1");
        final var dsib2 = new Dsib();
        dsib2.setFilename("L2");
        final var dsib3 = new Dsib();
        dsib3.setFilename("L3");
        final var dsib4 = new Dsib();
        dsib4.setFilename("L4");
        final var dsib5 = new Dsib();
        dsib5.setFilename("L5");
        final var dsib6 = new Dsib();
        dsib6.setFilename("L6");

        entityIngestor.process((e) -> List.of(dsib1, dsib2, dsib3, dsib4, dsib5, dsib6));

        Map<String, Object> data = Map.of(
                "list", List.of(
                        Map.of("filename","L1"),
                        Map.of("filename","L2"),
                        Map.of("filename","L3"),
                        Map.of("filename","L4"),
                        Map.of("filename","L5"),
                        Map.of("filename","L6")
                )
        );
        final var dataTest = new DataTest(data);
        final var mapping = new Mapping();
        mapping.setFrom("test.data[list][filename]");
        mapping.setTo(new BeanProperty("dsib.filename"));

        final var processorDescription = new ProcessorDescription();
        processorDescription.setName("Dsib");
        processorDescription.setEntityMetadata(EntityFactory.getInstance().getMetadata(Dsib.class));
        processorDescription.setMappings(List.of(mapping));
        final var processor = new DefaultProcessor(processorDescription, entityIngestor);

        // When
        final var res= processor.apply(BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(dataTest)));

        // Then
        assertThat(res)
                .hasSize(6)
                .allMatch(e -> ((Dsib)e.getEntity()).getId() != null);
    }

    public record DataTest (
            Map<String, Object> data
    ){

    }

}