package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChunkProcessor extends DefaultProcessor<Chunk> {
    private static final String DSIB_EXTENSION = "DSIB.xml";

    public ChunkProcessor (
            String entityName,
            String configurationName,
            List<Mapping> mappings,
            List<BeanProperty> dependencies,
            EntityFinder entityFinder
    ) {
        super(entityName, configurationName, mappings, dependencies, entityFinder);
    }

    @Override
    public List<Chunk> apply(BeanAccessor bean) {
        final var processedEntities = super.apply(bean);

        final var missingDsibByFilename = processedEntities.stream()
                .filter(c -> c.getDsib().getId() == null)
                .peek(this::setDefaultDsibValue)
                .collect(Collectors.groupingBy(c -> c.getDsib().getFilename()));

        final var spec = Specification.<Dsib>where(null).and(
                EntitySpecification.getEntityBy("filename", missingDsibByFilename.keySet()));
        entityFinder.findAll(spec, Dsib.class)
                .forEach(d -> {
                     missingDsibByFilename.get(d.getFilename())
                            .forEach(c -> c.setDsib((Dsib)d));
                });

        return processedEntities;
    }

    private void setDefaultDsibValue(Chunk chunk) {
        chunk.getDsib().setFilename(chunkToDsibFilename(chunk.getFilename()));
        chunk.getDsib().setMission(chunk.getMission());
    }

    private String chunkToDsibFilename(String chunkFilename) {
        String regex = "^.+(?=DSDB)";
        Matcher m = Pattern.compile(regex).matcher(chunkFilename);

        if (m.find()) {
            return m.group(0) + DSIB_EXTENSION;
        } else{
            throw new IllegalStateException("No pattern found into chunk filename given.");
        }
    }
}
