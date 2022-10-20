package eu.csgroup.coprs.monitoring.traceingestor.association;

import eu.csgroup.coprs.monitoring.common.ingestor.EntityFinder;
import eu.csgroup.coprs.monitoring.traceingestor.entity.EntityProcessing;

import java.util.List;

public interface EntityAssociation {
    List<EntityProcessing> associate(EntityProcessing entity, List<EntityProcessing> currentReferences, EntityFinder entityFinder);
}
