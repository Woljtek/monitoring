package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.ReloadableBeanFactory;
import eu.csgroup.coprs.monitoring.common.datamodel.*;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import eu.csgroup.coprs.monitoring.common.message.FilteredTrace;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@Import(TraceIngestorConfiguration.class)
@ContextConfiguration(initializers = TestInitializer.class)
@DataJpaTest
// Comment the two below annotation to test with non embedded database
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("dev-embedded")
//@ActiveProfiles("dev-integration")
public class DuplicateProcessingTests {

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
    //@Rollback(value = false)
    public void testChunkAsInputQuery () {
        // Given
        entityIngestor.process(this::getDataSet);
        final var sink = conf.traceIngestor(factory, entityIngestor);

        final var input = List.of(
                "DCS_chunk_DSDB.raw",
                "dsib_DSIB.xml"
        );

        final var output = List.of(
                "l0-0"
        );

        final var processingRef = getProcessingRef("duplicate_processing", input, output);
        processingRef.getLog().getTrace().getHeader().setRsChainName("input_list_external");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        final var processing = entityIngestor.findAll(Processing.class);
        assertThat(processing)
                .hasSize(13)
                .filteredOn(proc -> ! proc.getRsChainName().equals("input_list_external"))
                .hasSize(12)
                .allMatch(Processing::isDuplicate);
        assertThat(processing)
                .filteredOn(proc -> proc.getRsChainName().equals("input_list_external"))
                .allMatch(proc -> ! proc.isDuplicate());
    }

    @Test
    public void testChunkAsInputQueryWithRollback () {
        // Given
        entityIngestor.process(this::getDataSet);
        final var chunk = entityIngestor.findEntityBy(Chunk.class, Map.of("filename", "DCS_chunk_DSDB.raw")).get(0);

        // When
        final var exception = assertThatThrownBy(() -> {
            entityIngestor.process((e) -> {

                        e.setDuplicateProcessing(
                                "processing.id in (select ile.processing_id from input_list_external ile where ile.external_input_id in ?1)",
                                List.of(
                                        List.of(chunk.getId())
                                )
                        );

                        // Create artificial exception to check rollback
                        e.setDuplicateProcessing("", List.of());
                        return List.of(chunk);
                    });
                });

        // Then
        exception.isNotNull();
        assertThat(entityIngestor.findAll(Processing.class))
                .allMatch(proc -> ! proc.isDuplicate());
    }

    @Test
    public void testInputListInternalAsInputQuery () {
        // Given
        entityIngestor.process(this::getDataSet);
        final var sink = conf.traceIngestor(factory, entityIngestor);

        final var input = List.of(
                "l2-0"
        );

        final var output = List.of(
                "l3-0"
        );
        final var nonDuplicateProc = List.of("l0", "l1", "l2");

        final var processingRef = getProcessingRef("duplicate_processing", input, output);
        processingRef.getLog().getTrace().getHeader().setRsChainName("input_list_internal");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(Processing.class))
                .filteredOn(Processing::isDuplicate)
                .hasSize(9)
                .allMatch(proc -> ! nonDuplicateProc.contains(proc.getRsChainName()));
    }

    @Test
    public void testInputListInternalAsInputQueryWithBranch () {
        // Given
        entityIngestor.process(this::getDataSet);
        final var sink = conf.traceIngestor(factory, entityIngestor);

        final var input = List.of(
                "l6-0"
        );

        final var output = List.of(
                "l7ab-0",
                "l7-0"
        );
        final var duplicateProc = List.of("l7", "l8", "l8ab", "l9", "l9ab");

        final var processingRef = getProcessingRef("duplicate_processing", input, output);
        processingRef.getLog().getTrace().getHeader().setRsChainName("input_list_internal");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(Processing.class))
                .filteredOn(Processing::isDuplicate)
                .hasSize(5)
                .allMatch(proc -> duplicateProc.contains(proc.getRsChainName()));
    }

    @Test
    public void testOutputListAsInputQuery () {
        // Given
        entityIngestor.process(this::getDataSet);
        final var sink = conf.traceIngestor(factory, entityIngestor);

        final var input = List.of(
                "l2-0"
        );

        final var output = List.of(
                "l3-0"
        );
        final var nonDuplicateProc = List.of("l0", "l1", "l2");

        final var processingRef = getProcessingRef("duplicate_processing", input, output);
        processingRef.getLog().getTrace().getHeader().setRsChainName("l3");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(Processing.class))
                .filteredOn(Processing::isDuplicate)
                .hasSize(9)
                .allMatch(proc -> ! nonDuplicateProc.contains(proc.getRsChainName()));
    }

    @Test
    public void testOutputListAsInputQueryWithWrongProcessorName () {
        // Given
        entityIngestor.process(this::getDataSet);
        final var sink = conf.traceIngestor(factory, entityIngestor);

        final var input = List.of(
                "l2-0"
        );

        final var output = List.of(
                "l3-0"
        );
        final var nonDuplicateProc = List.of("l0", "l1", "l2");

        final var processingRef = getProcessingRef("duplicate_processing", input, output);
        processingRef.getLog().getTrace().getHeader().setRsChainName("l2");

        // When
        sink.accept(toMessage(processingRef));

        // Then
        assertThat(entityIngestor.findAll(Processing.class))
                .hasSize(13)
                .allMatch(proc -> ! proc.isDuplicate());
    }

    public List<DefaultEntity> getDataSet (EntityIngestor entityIngestor) {
        final var allEntities = new ArrayList<DefaultEntity>();

        final var dsib = new Dsib();
        dsib.setFilename("dsib_DSIB.xml");
        allEntities.add(dsib);

        final var chunk = new Chunk();
        chunk.setFilename("DCS_chunk_DSDB.raw");
        chunk.setDsib(dsib);
        allEntities.add(chunk);

        var currentProcessing = new Processing();
        currentProcessing.setProcessingDate(Instant.parse("2022-10-28T10:23:00.00Z"));
        currentProcessing.setRsChainName("l0");
        allEntities.add(currentProcessing);

        allEntities.add(new InputListExternal(new InputListExternalId(dsib, currentProcessing)));
        allEntities.add(new InputListExternal(new InputListExternalId(chunk, currentProcessing)));

        final var nextInput = new ArrayList<Product>();
        final var intermediateInput = new ArrayList<Product>();

        for (int index = 0; index < 10; index++) {
            if (! nextInput.isEmpty()) {
                currentProcessing = new Processing();
                currentProcessing.setRsChainName("l%s".formatted(index));
                currentProcessing.setProcessingDate(Instant.parse("2022-10-28T10:23:00.00Z"));
                allEntities.add(currentProcessing);

                for (var inputProduct : nextInput) {
                    allEntities.add(new InputListInternal(new InputListInternalId(currentProcessing, inputProduct)));
                }
            }

            // Create second branch
            if (index > 7) {
                var isolatedProcessing = new Processing();
                isolatedProcessing.setRsChainName("l%sab".formatted(index));
                isolatedProcessing.setProcessingDate(Instant.parse("2022-10-28T10:23:00.00Z"));
                allEntities.add(isolatedProcessing);


                for (var inputProduct : intermediateInput) {
                    allEntities.add(new InputListInternal(new InputListInternalId(isolatedProcessing, inputProduct)));
                }
                intermediateInput.clear();

                var random = new Random();
                var outputNumber = random.nextInt(1,10);
                for (var outputIndex = 0; outputIndex < outputNumber; outputIndex++) {
                    final var outputProduct = new Product();
                    outputProduct.setFilename("l%sab-%s".formatted(index, outputIndex));
                    allEntities.add(outputProduct);
                    intermediateInput.add(outputProduct);

                    allEntities.add(new OutputList(new OutputListId(isolatedProcessing, outputProduct)));

                }
            }

            nextInput.clear();

            var random = new Random();
            var outputNumber = random.nextInt(1,10);
            for (var outputIndex = 0; outputIndex < outputNumber; outputIndex++) {
                final var outputProduct = new Product();
                outputProduct.setFilename("l%s-%s".formatted(index, outputIndex));
                allEntities.add(outputProduct);
                nextInput.add(outputProduct);

                allEntities.add(new OutputList(new OutputListId(currentProcessing, outputProduct)));

            }

            if (index == 7) {
                outputNumber = random.nextInt(1,10);
                for (var outputIndex = 0; outputIndex < outputNumber; outputIndex++) {
                    final var outputProduct = new Product();
                    outputProduct.setFilename("l%sab-%s".formatted(index, outputIndex));
                    allEntities.add(outputProduct);
                    intermediateInput.add(outputProduct);

                    allEntities.add(new OutputList(new OutputListId(currentProcessing, outputProduct)));

                }
            }
        }

        return allEntities;
    }

    private Message<FilteredTrace> toMessage(FilteredTrace ref) {
        return new GenericMessage<>(ref);
    }

    private FilteredTrace getProcessingRef (String filterName, List<String> inputFilename, List<String> outputFilename) {
        final var header = new Header();
        header.setType(TraceType.REPORT);
        header.setMission("S2");
        header.setLevel(Level.INFO);
        header.setWorkflow(Workflow.NOMINAL);

        final var task = new EndTask();
        //task.setSatellite("S2B");

        final var output = new HashMap<String, Object>();
        output.put("filename_strings", outputFilename);
        task.setOutput(output);

        final var input = new HashMap<String, Object>();
        input.put("filename_strings", inputFilename);
        task.setInput(input);

        final var trace = new Trace();
        trace.setHeader(header);
        trace.setTask(task);

        final var custom = new HashMap<String, Object>();
        custom.put("key1", "value1");
        custom.put("key2", "value2");
        custom.put("key3", "value3");
        trace.setCustom(custom);

        final var traceLog = new TraceLog();
        traceLog.setTrace(trace);

        return new FilteredTrace(filterName, traceLog);
    }
}
