package eu.csgroup.coprs.monitoring.common;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFactory;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityMetadata;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityFactoryTest {

    @Test
    public void testNominal () {
        // Given
        final var factory = EntityFactory.getInstance();

        // When
        final var chunkMetadata = factory.getMetadata(Chunk.class);
        final var chunkRelyOn = mapRelyOn(chunkMetadata);
        final var chunkDependencies = mapDependencies(chunkMetadata);

        final var externalInputMetadata = factory.getMetadata(ExternalInput.class);
        final var externalInputChild = externalInputMetadata.getChild();
        final var externalInputDependencies = mapDependencies(externalInputMetadata);

        final var inputListExternalMetadata = factory.getMetadata(InputListExternal.class);
        final var inputLIstExternalRelyOn = mapRelyOn(inputListExternalMetadata);

        final var noMetadata = factory.getMetadata(OutputListId.class);

        // Then
        assertThat(chunkMetadata)
                .matches(metadata -> metadata.getChild().size() == 0)
                .matches(metadata -> metadata.getReferencedBy().size() == 0)
                .matches(metadata -> Chunk.class.getSimpleName().equals(metadata.getEntityName()))
                .matches(metadata -> Chunk.class.equals(metadata.getEntityClass()));
        assertThat(chunkRelyOn).hasSize(1).allMatch(entity -> entity.equals(Dsib.class));
        assertThat(chunkDependencies).hasSize(1).allMatch(String.class::equals);

        assertThat(externalInputMetadata)
                .matches(metadata -> ExternalInput.class.getSimpleName().equals(metadata.getEntityName()))
                .matches(metadata -> ExternalInput.class.equals(metadata.getEntityClass()));
        assertThat(externalInputMetadata.getReferencedBy()).hasSize(1).allMatch(entity -> entity.equals(InputListExternal.class));
        assertThat(externalInputChild).hasSize(3).contains(AuxData.class, Dsib.class, Chunk.class);
        assertThat(externalInputMetadata.getRelyOn()).isEmpty();
        assertThat(externalInputDependencies).hasSize(1).allMatch(String.class::equals);

        assertThat(inputListExternalMetadata)
                .matches(metadata -> InputListExternal.class.getSimpleName().equals(metadata.getEntityName()))
                .matches(metadata -> InputListExternal.class.equals(metadata.getEntityClass()))
                .matches(metadata -> metadata.getChild().isEmpty())
                .matches(metadata -> metadata.getDependencies().isEmpty())
                .matches(metadata -> metadata.getReferencedBy().isEmpty());
        assertThat(inputLIstExternalRelyOn).hasSize(2).contains(ExternalInput.class, Processing.class);

        assertThat(noMetadata).isNull();
    }

    // ------

    private Stream<Class<? extends DefaultEntity>> mapRelyOn (EntityMetadata metadata) {
        return metadata.getRelyOn()
                .keySet()
                .stream()
                .map(EntityMetadata::getEntityClass);

    }

    private Stream<Class<?>> mapDependencies (EntityMetadata metadata) {
        return metadata.getDependencies()
                .stream()
                .map(Field::getType);
    }
}
