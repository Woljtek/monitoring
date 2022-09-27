package eu.csgroup.coprs.monitoring.common;

import eu.csgroup.coprs.monitoring.common.bean.AutoMergeableMap;
import eu.csgroup.coprs.monitoring.common.datamodel.Level;
import eu.csgroup.coprs.monitoring.common.datamodel.Status;
import eu.csgroup.coprs.monitoring.common.datamodel.Workflow;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityTest {
    @Test
    public void testAuxData() {
        // Given
        final var auxData = new AuxData();
        auxData.setId(1L);

        // When
        final var copy = auxData.copy();

        // Then
        assertThat(auxData)
                .isEqualTo(copy)
                .hasToString(copy.toString());
    }

    @Test
    public void testChunk() {
        // Given
        final var chunk = new Chunk();
        chunk.setId(1L);

        // When
        final var copy = chunk.copy();

        // Then
        assertThat(chunk)
                .isEqualTo(copy)
                .hasToString(copy.toString());
    }

    @Test
    public void testDsib() {
        // Given
        final var dsib = new Dsib();
        dsib.setId(1L);

        // When
        final var copy = dsib.copy();

        // Then
        assertThat(dsib)
                .isEqualTo(copy)
                .hasToString(copy.toString());
    }

    @Test
    public void testExternalInput() {
        // Given
        final var externalInput = getExternalInput();
        final var allArgsConstructor = new ExternalInput(
                1L,
                "external input",
                "S2",
                Instant.parse("2022-10-13T10:23:59.00Z"),
                Instant.parse("2022-10-14T10:23:59.00Z"),
                Instant.parse("2022-10-15T10:23:59.00Z"),
                Instant.parse("2022-10-16T10:23:59.00Z"),
                new AutoMergeableMap()
        );

        // When
        final var copy = externalInput.copy();
        final var duplicated = externalInput.copy();
        duplicated.resetId();

        // Then
        assertThat(externalInput)
                .isEqualTo(allArgsConstructor)
                .isEqualTo(copy)
                .hasToString(copy.toString())
                .isNotEqualTo(duplicated);
    }

    @Test
    public void testInputListExternal() {
        // Given
        final var inputListExternal = new InputListExternal();
        inputListExternal.getId().setExternalInput(getExternalInput());
        inputListExternal.getId().setProcessing(getProcessing());

        final var allArgsConstructor = new InputListExternal(new InputListExternalId(getExternalInput(), getProcessing()));

        // When
        final var copy = inputListExternal.copy();
        final var duplicated = inputListExternal.copy();
        duplicated.resetId();

        // Then
        assertThat(inputListExternal)
                .isEqualTo(allArgsConstructor)
                .isEqualTo(copy)
                .hasToString(copy.toString())
                .isNotEqualTo(duplicated);
    }

    @Test
    public void testInputListInternal() {
        // Given
        final var inputListInternal = new InputListInternal();
        inputListInternal.getId().setProduct(getProduct());
        inputListInternal.getId().setProcessing(getProcessing());

        final var allArgsConstructor = new InputListInternal(new InputListInternalId(getProcessing(), getProduct()));

        // When
        final var copy = inputListInternal.copy();
        final var duplicated = inputListInternal.copy();
        duplicated.resetId();

        // Then
        assertThat(inputListInternal)
                .isEqualTo(allArgsConstructor)
                .isEqualTo(copy)
                .hasToString(copy.toString())
                .isNotEqualTo(duplicated);
    }

    @Test
    public void testMissingProducts() {
        // Given
        final var missingProducts = new MissingProducts();
        missingProducts.setId(1L);
        missingProducts.setProcessing(getProcessing());
        missingProducts.setProductMetadataCustom(new AutoMergeableMap());
        missingProducts.setEstimatedCount(10);
        missingProducts.setDuplicate(false);
        missingProducts.setEndToEndProduct(false);

        final var allArgsConstructor = new MissingProducts(
                1L,
                getProcessing(),
                new AutoMergeableMap(),
                10,
                false,
                false
        );

        // When
        final var copy = missingProducts.copy();
        final var duplicated = missingProducts.copy();
        duplicated.resetId();

        // Then
        assertThat(missingProducts)
                .isEqualTo(allArgsConstructor)
                .isEqualTo(copy)
                .hasToString(copy.toString())
                .isNotEqualTo(duplicated);
    }

    @Test
    public void testOutputList() {
        // Given
        final var outputList = new OutputList();
        outputList.getId().setProduct(getProduct());
        outputList.getId().setProcessing(getProcessing());

        final var allArgsConstructor = new OutputList(new OutputListId(getProcessing(), getProduct()));

        // When
        final var copy = outputList.copy();
        final var duplicated = outputList.copy();
        duplicated.resetId();

        // Then
        assertThat(outputList)
                .isEqualTo(allArgsConstructor)
                .isEqualTo(copy)
                .hasToString(copy.toString())
                .isNotEqualTo(duplicated);
    }

    @Test
    public void testProcessing() {
        // Given
        final var processing = getProcessing();
        final var allArgsConstructor = new Processing(
                1L,
                "S2",
                "Rs chain name",
                "Rs chain version",
                Workflow.NOMINAL,
                Level.INFO,
                Status.OK,
                Instant.parse("2022-10-13T10:23:59.00Z"),
                Instant.parse("2022-10-14T10:23:59.00Z"),
                Instant.parse("2022-10-15T10:23:59.00Z"),
        false);

        // When
        final var copy = processing.copy();
        final var duplicated = processing.copy();
        duplicated.resetId();

        // Then
        assertThat(processing)
                .isEqualTo(allArgsConstructor)
                .isEqualTo(copy)
                .hasToString(copy.toString())
                .isNotEqualTo(duplicated);
    }

    @Test
    public void testProduct() {
        // Given
        final var product = getProduct();
        final var allArgsConstructor = new Product(
                1L,
                "product",
                new AutoMergeableMap(),
                "Timeliness name",
                10,
                false,
                false,
                Instant.parse("2022-10-13T10:23:59.00Z"),
                Instant.parse("2022-10-14T10:23:59.00Z"),
                false
        );

        // When
        final var copy = product.copy();
        final var duplicated = product.copy();
        duplicated.resetId();

        // Then
        assertThat(product)
                .isEqualTo(allArgsConstructor)
                .isEqualTo(copy)
                .hasToString(copy.toString())
                .isNotEqualTo(duplicated);
    }

    // ------
    private ExternalInput getExternalInput () {
        final var externalInput = new ExternalInput();
        externalInput.setId(1L);
        externalInput.setFilename("external input");
        externalInput.setCustom(new AutoMergeableMap());
        externalInput.setMission("S2");
        externalInput.setPickupPointSeenDate(Instant.parse("2022-10-13T10:23:59.00Z"));
        externalInput.setPickupPointAvailableDate(Instant.parse("2022-10-14T10:23:59.00Z"));
        externalInput.setIngestionDate(Instant.parse("2022-10-15T10:23:59.00Z"));
        externalInput.setCatalogStorageDate(Instant.parse("2022-10-16T10:23:59.00Z"));

        return externalInput;
    }

    private Processing getProcessing () {
        final var processing = new Processing();
        processing.setId(1L);
        processing.setMission("S2");
        processing.setRsChainName("Rs chain name");
        processing.setRsChainVersion("Rs chain version");
        processing.setWorkflow(Workflow.NOMINAL);
        processing.setLevel(Level.INFO);
        processing.setStatus(Status.OK);
        processing.setProcessingDate(Instant.parse("2022-10-13T10:23:59.00Z"));
        processing.setEndSensingDate(Instant.parse("2022-10-14T10:23:59.00Z"));
        processing.setT0PdgsDate(Instant.parse("2022-10-15T10:23:59.00Z"));
        processing.setDuplicate(false);

        return processing;
    }

    private Product getProduct () {
        final var product = new Product();
        product.setId(1L);
        product.setFilename("product");
        product.setCustom(new AutoMergeableMap());
        product.setTimelinessName("Timeliness name");
        product.setTimelinessValueSeconds(10);
        product.setEndToEndProduct(false);
        product.setDuplicate(false);
        product.setT0PdgsDate(Instant.parse("2022-10-13T10:23:59.00Z"));
        product.setPripStorageDate(Instant.parse("2022-10-14T10:23:59.00Z"));
        product.setLate(false);

        return product;
    }
}
