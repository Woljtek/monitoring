package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
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

    private static final String FILENAME_MATCH = "^.+(?=DSDB)";

    private static final Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_MATCH);

    private static final String FILENAME_REPLACE = "%1$s" + DSIB_EXTENSION;

    // Re-use retrieved or created dsib to not to have to do each time it's requested (one dsib for many chunk).
    private final Map<String, Dsib> cache = new HashMap<>();

    public ChunkToDsibAssociation (Deque<Field> associationFields) {
        super(associationFields);
    }

    @Override
    public List<Chunk> associate(Chunk entityContainer, List<Dsib> references, EntityFinder entityFinder) {
        final var dsibFilename = chunkToDsibFilename(entityContainer.getFilename());

        return references.stream()
                // Keep dsib where filename match to requested one.
                .filter(dsib -> dsibFilename.equals(dsib.getFilename()))
                .findFirst()
                // If no one found in list of references, find it in db or create one.
                .or(() -> Optional.of(createDsibFromChunk(entityContainer, entityFinder)))
                // Set dsib reference in chunk object
                .map(dsib -> associate(entityContainer, dsib, false))
                .map(List::of)
                .orElse(List.of());
    }

    /**
     * Use the following mechanism and in the order to retrieve dsib instance:
     * <ul>
     *     <li>cache</li>
     *     <li>database</li>
     *     <li>constructor</li>
     * </ul>
     *
     * @param chunk Chunk data that will be used to create dsib instance (constructor case)
     * @param entityFinder Instance that will process search in database.
     * @return dsib instance to associated with chunk.
     */
    private Dsib createDsibFromChunk (Chunk chunk, EntityFinder entityFinder) {
        final var dsibFilename = chunkToDsibFilename(chunk.getFilename());

        // Check if dsib was already associated to an other chunk
        var dsib = cache.get(dsibFilename);

        // If not create find it or create it.
        if (dsib == null) {
            // Find in database
            final var dbDsib = entityFinder.findAll(
                        Specification.<Dsib>where(null).and(EntitySpecification.getEntityBy("filename", dsibFilename)),
                        Dsib.class
                    ).stream()
                    .findFirst();

            if (dbDsib.isPresent()) {
                dsib = dbDsib.get();
                cache.put(dsibFilename, dsib);
            } else {
                // If not found in db create a new one.
                dsib = new Dsib();
                dsib.setFilename(dsibFilename);
                dsib.setMission(chunk.getMission());

                cache.put(dsibFilename, dsib);
            }
        }

        return dsib;
    }

    /**
     * Convert chunk filename to dsib file name
     *
     * @param chunkFilename Chunk file name
     * @return dsib file name base on chunk file name.
     */
    private static String chunkToDsibFilename(String chunkFilename) {
        final var value = ConversionUtil.convert(FILENAME_PATTERN, FILENAME_REPLACE, chunkFilename);

        if (value == null) {
            throw new IllegalStateException("No pattern found into chunk filename given.");
        }

        return value;
    }
}
