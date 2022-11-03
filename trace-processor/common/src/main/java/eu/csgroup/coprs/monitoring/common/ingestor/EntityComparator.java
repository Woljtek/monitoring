package eu.csgroup.coprs.monitoring.common.ingestor;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class EntityComparator implements Comparator<Class<? extends DefaultEntity>> {

    @Override
    public int compare(Class<? extends DefaultEntity> e1, Class<? extends DefaultEntity> e2) {
        log.debug("Compare %s with %s".formatted(e1.getSimpleName(), e2.getSimpleName()));
        final var metadata1 = EntityFactory.getInstance().getMetadata(e1);
        final var metadata2 = EntityFactory.getInstance().getMetadata(e2);

        if (metadata1.getRelyOn().keySet().contains(metadata2)
                || metadata1.getChild().contains(metadata2.getEntityClass())
                || metadata1.getRelyOn().keySet().stream().flatMap(m -> m.getChild().stream()).toList().contains(metadata2.getEntityClass())) {
            return 1;
        } else if (metadata2.getRelyOn().keySet().contains(metadata1)
                || metadata2.getChild().contains(metadata1.getEntityClass())
                || metadata2.getRelyOn().keySet().stream().flatMap(m -> m.getChild().stream()).toList().contains(metadata1.getEntityClass())
                || metadata1.getReferencedBy().contains(metadata2.getEntityClass())) {
            return -1;
        } else if (! metadata1.getRelyOn().isEmpty() || ! metadata2.getReferencedBy().isEmpty()) {
            return 1;
        } else if (! metadata2.getRelyOn().isEmpty() || ! metadata1.getReferencedBy().isEmpty()) {
            return -1;
        } else {
            return 0;
        }
    }
}
