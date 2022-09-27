package eu.csgroup.coprs.monitoring.common;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityException;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityHelper;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EntityHelperTest {
    @Test
    public void testResolveEntityClass () {
        // Given
        final var classNameWithoutPackage = "Chunk";
        final var classNameWithPackage = "%s.%s".formatted(EntityIngestor.BASE_PACKAGE, classNameWithoutPackage);
        final var nonExistingEntityClassName = "Test";

        // When
        final var classFromWithoutPackage = EntityHelper.getEntityClass(classNameWithoutPackage);
        final var classFromWithPackage = EntityHelper.getEntityClass(classNameWithPackage);
        final var nonExistingClass = assertThatThrownBy(() -> EntityHelper.getEntityClass(nonExistingEntityClassName));

        // Then
        assertThat(classFromWithoutPackage)
                .isEqualTo(Chunk.class)
                .isEqualTo(classFromWithPackage);

        nonExistingClass.isNotNull()
                .isInstanceOf(EntityException.class);
    }

    @Test
    public void testCopy () {
        // Given
        final var externalInput = new ExternalInput();
        final var outputList = new OutputList();
        outputList.getId().setProcessing(new Processing());
        outputList.getId().setProduct(new Product());
        externalInput.setId(1L);

        // When
        final var copy = EntityHelper.copy(externalInput);
        final var duplicated = EntityHelper.copy(externalInput, true);

        final var outputListDuplicated = EntityHelper.copy(outputList, true);

        // Then
        assertThat(externalInput)
                .isEqualTo(copy)
                .isNotEqualTo(duplicated);

        assertThat(duplicated.getId()).isNull();

        assertThat(outputList).isNotEqualTo(outputListDuplicated);
    }
}
