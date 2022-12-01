package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.Chunk;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.Dsib;
import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.common.jpa.EntitySpecification;
import eu.csgroup.coprs.monitoring.traceingestor.converter.FormatAction;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityState;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

public class ChunkToDsibAssociation extends DefaultAssociation {
    private static final String DSIB_EXTENSION = "DSIB.xml";

    private static final String FILENAME_MATCH = "^.+(?=DSDB)";

    private static final Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_MATCH);

    private static final String FILENAME_REPLACE = "%1$s" + DSIB_EXTENSION;

    // Re-use retrieved or created dsib to not to have to do each time it's requested (one dsib for many chunk).
    private final Map<String, EntityProcessing> cache = new HashMap<>();

    public ChunkToDsibAssociation (Deque<Field> associationFields) {
        super(associationFields);
    }

    @Override
    public List<EntityProcessing> associate(EntityProcessing entityContainer, List<EntityProcessing> references, EntityFinder entityFinder) {
        final var dsibFilename = chunkToDsibFilename(((Chunk)entityContainer.getEntity()).getFilename());

        return references.stream()
                // Keep dsib where filename match to requested one.
                .filter(dsib -> dsibFilename.equals(((Dsib)dsib.getEntity()).getFilename()))
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
     * @param chunkContainer Chunk data that will be used to create dsib instance (constructor case)
     * @param entityFinder Instance that will process search in database.
     * @return dsib instance to associated with chunk.
     */
    private EntityProcessing createDsibFromChunk (EntityProcessing chunkContainer, EntityFinder entityFinder) {
        final var dsibFilename = chunkToDsibFilename(((Chunk)(chunkContainer.getEntity())).getFilename());

        // Check if dsib was already associated to an other chunk
        var dsibProc = cache.get(dsibFilename);

        // If not create find it or create it.
        if (dsibProc == null) {
            // Find in database
            final var dbDsib = entityFinder.findAll(
                            Specification.<Dsib>where(null).and(EntitySpecification.getEntityBy("filename", dsibFilename)),
                            Dsib.class
                    ).stream()
                    .findFirst();

            if (dbDsib.isPresent()) {
                dsibProc = EntityProcessing.fromEntity(dbDsib.get(), EntityState.UNCHANGED);
                cache.put(dsibFilename, dsibProc);
            } else {
                // If not found in db create a new one.
                var dsib = new Dsib();
                dsib.setFilename(dsibFilename);
                dsib.setMission(((Chunk)(chunkContainer.getEntity())).getMission());

                dsibProc = EntityProcessing.fromEntity(dsib);

                cache.put(dsibFilename, dsibProc);
            }
        }

        return dsibProc;
    }

    /**
     * Convert chunk filename to dsib file name
     *
     * @param chunkFilename Chunk file name
     * @return dsib file name base on chunk file name.
     */
    private static String chunkToDsibFilename(String chunkFilename) {
        final var value = new FormatAction("FORMAT " + FILENAME_PATTERN + " " + FILENAME_REPLACE).execute(List.of(chunkFilename));

        if (value == null) {
            throw new IllegalStateException("No pattern found into chunk filename given.");
        }

        return (String) value;
    }
}