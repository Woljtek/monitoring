package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.EndTask;
import eu.csgroup.coprs.monitoring.common.datamodel.Header;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
import eu.csgroup.coprs.monitoring.traceingestor.config.Mapping;
import eu.csgroup.coprs.monitoring.traceingestor.entity.DefaultHandler;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import eu.csgroup.coprs.monitoring.traceingestor.mapper.TraceMapper;
import org.junit.Test;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceMapperTests {
    @Test
    public void testRemoveEntityIfNull () {
        // Given
        final var header = new Header();
        final var mapping = new Mapping();
        mapping.setFrom("header.mission");
        mapping.setTo(new BeanProperty("chunk.mission"));
        final var handler = new DefaultHandler(Chunk.class);
        final var mapper = new TraceMapper(BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(header)), "End Task");

        // When
        final var someEntity = mapper.map(List.of(mapping), handler);
        mapping.setRemoveEntityIfNull(true);
        final var noEntity = mapper.map(List.of(mapping), handler);

        // Then
        assertThat(someEntity).isNotEmpty();
        assertThat(noEntity).isEmpty();
    }

    @Test
    public void testSetValueOnlyIfNull () {
        // Given
        final var header = new Header();
        header.setMission("S2");
        final var mapping = new Mapping();
        mapping.setFrom("header.mission");
        mapping.setSetValueOnlyIfNull(true);
        mapping.setTo(new BeanProperty("chunk.mission"));
        final var handler = new DefaultHandler(Chunk.class);
        final var mapper = new TraceMapper(BeanAccessor.from(PropertyAccessorFactory.forBeanPropertyAccess(header)), "End Task");

        // When
        final var newEntity = mapper.map(List.of(mapping), handler);

        header.setMission("S1");
        final var copy = ((Chunk)(newEntity.get(0).getDelegate().getWrappedInstance())).copy();
        handler.setDefaultEntities(List.of(EntityProcessing.fromEntity(copy)), List.of(mapping));
        final var notUpdatedEntity = mapper.map(List.of(mapping), handler);

        // Then
        assertThat((Chunk)newEntity.get(0).getDelegate().getWrappedInstance())
                .isEqualTo(notUpdatedEntity.get(0).getDelegate().getWrappedInstance())
                .extracting(Chunk::getMission)
                .isEqualTo("S2");
        assertThat((Chunk)notUpdatedEntity.get(0).getDelegate().getWrappedInstance())
                .extracting(Chunk::getMission)
                .isEqualTo("S2");
    }
}
