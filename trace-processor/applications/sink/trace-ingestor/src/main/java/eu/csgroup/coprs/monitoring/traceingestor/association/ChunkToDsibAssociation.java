package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.entity.ConversionUtil;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

public class ChunkToDsibAssociation extends DefaultAssociation<Chunk, Dsib> {
    private static final String DSIB_EXTENSION = "DSIB.xml";

    private static String FILENAME_MATCH = "^.+(?=DSDB)";

    private static Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_MATCH);

    private static String FILENAME_REPLACE = "%1$s" + DSIB_EXTENSION;

    private Map<String, Dsib> cache = new HashMap<>();

    public ChunkToDsibAssociation (Class containerClass, Deque<Field> associationFields) {
        super(containerClass, associationFields);
    }

    @Override
    public List<DefaultEntity> associate(Chunk entityContainer, List<Dsib> references, EntityFinder entityFinder) {
        final var dsibFilename = chunkToDsibFilename(entityContainer.getFilename());

        return references.stream()
                .filter(dsib -> dsibFilename.equals(dsib.getFilename()))
                .findFirst()
                .or(() -> Optional.of(createDsibFromChunk(entityContainer, entityFinder)))
                .map(dsib -> associate(entityContainer, dsib, false))
                .map(chunk -> List.of(chunk))
                .get();
    }

    private Dsib createDsibFromChunk (Chunk chunk, EntityFinder entityFinder) {
        final var dsibFilename = chunkToDsibFilename(chunk.getFilename());

        var dsib = cache.get(dsibFilename);
        if (dsib == null) {
            final var dbDsib = entityFinder.findAll(
                        Specification.<Dsib>where(null).and(EntitySpecification.<Dsib>getEntityBy("filename", dsibFilename)),
                        Dsib.class
                    ).stream()
                    .findFirst();

            if (dbDsib.isPresent()) {
                dsib = dbDsib.get();
                cache.put(dsibFilename, dsib);
            } else {
                dsib = new Dsib();
                dsib.setFilename(dsibFilename);
                dsib.setMission(chunk.getMission());

                cache.put(dsibFilename, dsib);
            }
        }

        return dsib;
    }

    /**@Override
    public Ingestion getIngestionConfigFromContainer (Ingestion containerConfig) {
        final var filenameProperty = new BeanProperty("chunk.filename");
        final var missionProperty = new BeanProperty("chunk.mission");

        final var mappings = new LinkedList<Mapping>();
        containerConfig.getMappings()
                .stream()
                .filter(mapping -> mapping.getTo().equals(filenameProperty))
                .findFirst()
                .map(containerMapping -> {
                    final var mapping = new Mapping(containerMapping.getFrom(), new BeanProperty("dsib.filename"));
                    mapping.setMatch(FILENAME_MATCH);
                    mapping.setConvert(FILENAME_REPLACE);
                    return mapping;
                })
                .ifPresent(mappings::add);
        containerConfig.getMappings()
                .stream()
                .filter(mapping -> mapping.getTo().equals(missionProperty))
                .findFirst()
                .map(containerMapping -> {
                    return new Mapping(containerMapping.getFrom(), new BeanProperty("dsib.filename"));
                })
                .ifPresent(mappings::add);

        final var config = new Ingestion();
        config.setMappings(mappings);
        config.setName(containerConfig.getName());
        config.setDependencies(new LinkedList<>());

        return config;
    }*/

    private static String chunkToDsibFilename(String chunkFilename) {
        final var value = ConversionUtil.convert(FILENAME_PATTERN, FILENAME_REPLACE, chunkFilename);

        if (value == null) {
            throw new IllegalStateException("No pattern found into chunk filename given.");
        }

        return value;
    }
}
