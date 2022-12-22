package eu.csgroup.coprs.monitoring.traceingestor.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.DefaultEntity;
import eu.csgroup.coprs.monitoring.common.properties.PropertyUtil;
import lombok.Data;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class ClassStatistics {


    private String className;
    @JsonIgnore
    private Class<? extends DefaultEntity> entityClass;

    private int entitiesCreated;
    private int entitiesModified;
    private int unchangedEntities;

    private long processingTimeMs;
    private long ingestionTimeMs;

    public ClassStatistics(Class<? extends DefaultEntity> entityClass) {
        this.entityClass = entityClass;
        this.className = PropertyUtil.pascal2SnakeCasePropertyName(entityClass.getSimpleName());
        this.entitiesModified = 0;
        this.entitiesCreated = 0;
        this.unchangedEntities = 0;
    }

    public void incrementEntitiesCreated(int number) {
        this.entitiesCreated += number;
    }

    public void incrementEntitiesModified(int number) {
        this.entitiesModified += number;
    }

    public void incrementUnchangedEntities(int number) {
        this.unchangedEntities += number;
    }


}
