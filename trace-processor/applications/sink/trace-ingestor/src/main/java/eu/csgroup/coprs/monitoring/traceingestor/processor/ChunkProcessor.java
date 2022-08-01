package eu.csgroup.coprs.monitoring.traceingestor.processor;

import eu.csgroup.coprs.monitoring.common.bean.BeanAccessor;
import eu.csgroup.coprs.monitoring.common.bean.BeanProperty;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.ExternalInput;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.mapping.Mapping;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChunkProcessor extends DefaultProcessor<Chunk> {
    private static final String DSIB_EXTENSION = "DSIB.xml";

    public ChunkProcessor (
            String entityName,
            List<Mapping> mappings,
            List<BeanProperty> dependencies,
            BiFunction<Specification<? extends ExternalInput>, Class<? extends ExternalInput>, List<? extends ExternalInput>> entityFinder
    ) {
        super(entityName, mappings, dependencies, entityFinder);
    }

    @Override
    public List<Chunk> apply(BeanAccessor bean) {
        final var processedEntities = super.apply(bean);

        final var missingDsibByFilename = processedEntities.stream()
                .filter(c -> c.getDsib().getId() == null)
                .peek(c -> c.getDsib().setFilename(chunkToDsibFilename(c.getFilename())))
                .collect(Collectors.groupingBy(c -> c.getDsib().getFilename()));

        final var spec = Specification.<Dsib>where(null).and(
                EntitySpecification.getEntityBy("filename", missingDsibByFilename.keySet().iterator().next()));
        entityFinder.apply(spec, Dsib.class)
                .forEach(d -> {
                     missingDsibByFilename.get(d.getFilename())
                            .forEach(c -> c.setDsib((Dsib)d));
                });

        return processedEntities;
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
