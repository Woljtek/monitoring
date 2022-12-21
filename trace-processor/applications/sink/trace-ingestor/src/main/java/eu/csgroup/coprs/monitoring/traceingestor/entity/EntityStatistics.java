package eu.csgroup.coprs.monitoring.traceingestor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.DataBaseIngestionTimer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;

/**
 * Class used to register statistics about the behaviour of the TraceIngestion process
 * <br>
 * The finality is to display a json message whenever a trace is processed, containing these statistics.
 */
public class EntityStatistics {


    private int numberOfEntitiesInstanced;
    private int numberOfEntitiesModified;
    private int numberOfUnchangedEntities;

    private List<ClassStatistics> classStatistics;
    private long processingTime;
    private long ingestionTime;

    @JsonIgnore
    private Class<? extends DefaultEntity> currentClass;
    @JsonIgnore
    private static final EntityStatistics instance;


    static {
        instance= new EntityStatistics();
    }


    public static EntityStatistics getInstance() {

        return instance;
    }

    private EntityStatistics() {
        reset();
    }

    /**
     * Clears all the attributes in the class in order to record statistics for the next TraceLog
     */
    public void reset() {
        this.numberOfEntitiesInstanced = 0;
        this.numberOfEntitiesModified = 0;
        this.numberOfUnchangedEntities = 0;
        this.classStatistics = new ArrayList<>();
        this.classStatistics.add(new ClassStatistics(AuxData.class));
        this.classStatistics.add(new ClassStatistics(Chunk.class));
        this.classStatistics.add(new ClassStatistics(Dsib.class));
        this.classStatistics.add(new ClassStatistics(Product.class));
        this.classStatistics.add(new ClassStatistics(Processing.class));
        this.classStatistics.add(new ClassStatistics(InputListExternal.class));
        this.classStatistics.add(new ClassStatistics(InputListInternal.class));
        this.classStatistics.add(new ClassStatistics(OutputList.class));
        this.classStatistics.add(new ClassStatistics(MissingProducts.class));
    }

    /**
     * Records the number of entities created, modified, and unchanged, both globally and for each {@link DefaultEntity}
     * @param processedEntities list of entities processed by the {@link eu.csgroup.coprs.monitoring.traceingestor.processor.ProcessorOrchestrator}
     */
    public void incorporateEntities(Map<String, List<EntityProcessing>> processedEntities) {
        //from a map to a list
        processedEntities.entrySet()
                .stream().flatMap(entry -> entry.getValue().stream())
                //group by DefaultEntity
                .collect(groupingBy(EntityProcessing::getEntity))
                .forEach((defaultEntity, entites) -> {
                    //set the current Class to increment
                    setCurrentClass(defaultEntity.getClass());
                    //increment global amount, and the currentClass
                    for (EntityProcessing entity : entites) {
                        if (entity.getState().equals(EntityState.NEW)) {
                            incrementNumberOfEntitiesInstanced(1);
                        } else if (entity.getState().equals(EntityState.UPDATED)) {
                            incrementNumberOfEntitiesModified(1);
                        } else if (entity.getState().equals(EntityState.UNCHANGED)) {
                            incrementNumberOfUnchangedEntities(1);
                        }
                    }
                });
    }

    /**
     * Method called to set the ingestion times from the {@link DataBaseIngestionTimer} into the {@link EntityStatistics} class
     */
    private void pickUpIngestionTimes() {
        DataBaseIngestionTimer timer = DataBaseIngestionTimer.getInstance();

        this.ingestionTime = timer.resolveGlobalTimer();

        for (ClassStatistics stats : this.classStatistics) {
            stats.setIngestionTimeMs(timer.resolveUnitaryTimer(stats.getEntityClass()));
        }
    }


    public void retrieveProcessingTime(long milliSeconds, Class<? extends DefaultEntity> entityClass) {
        //no need to check for the presence of the Entry, I literally create them at class loading
        getClassStatisticsByClass(entityClass).ifPresent(statistics -> statistics.setProcessingTimeMs(milliSeconds));


    }

    public void retrieveIngestionTime(long milliSeconds, Class<? extends DefaultEntity> entityClass) {
        //no need to check for the presence of the Entry, I literally create them at class loading
        getClassStatisticsByClass(entityClass).ifPresent(statistics -> statistics.setIngestionTimeMs(milliSeconds));

    }

    @JsonIgnore
    public void setCurrentClass(Class<? extends DefaultEntity> current) {
        currentClass = current;
    }


    public Optional<ClassStatistics> getClassStatistics(DefaultEntity entity) {
        return this.classStatistics.stream()
                .filter(classStats -> classStats.getEntityClass().equals(entity.getClass()))
                .findAny();
    }

    private Optional<ClassStatistics> getClassStatisticsByClass(Class<? extends DefaultEntity> currentClass) {
        return this.classStatistics.stream()
                .filter(stat -> stat.getEntityClass().equals(currentClass))
                .findFirst();
    }

    public void incrementNumberOfEntitiesInstanced(int number) {
        this.numberOfEntitiesInstanced += number;

        Optional<ClassStatistics> currentOptional = getClassStatisticsByClass(this.currentClass);
        if (currentOptional.isPresent()) {
            var current = currentOptional.get();
            current.incrementEntitiesCreated(number);
        }
    }

    public void incrementNumberOfUnchangedEntities(int number) {
        this.numberOfUnchangedEntities += number;

        Optional<ClassStatistics> currentOptional = getClassStatisticsByClass(this.currentClass);
        if (currentOptional.isPresent()) {
            var current = currentOptional.get();
            current.incrementUnchangedEntities(number);
        }
    }


    public void incrementNumberOfEntitiesModified(int number) {
        this.numberOfEntitiesModified += number;

        Optional<ClassStatistics> currentOptional = getClassStatisticsByClass(this.currentClass);
        if (currentOptional.isPresent()) {
            var current = currentOptional.get();
            current.incrementEntitiesModified(number);
        }
    }


    private void removeClassStatisticsIfEmpty() {
        List<ClassStatistics> toRemove = new ArrayList<>();
        for (ClassStatistics item : this.classStatistics) {
            if (item.getEntitiesCreated() == 0 && item.getEntitiesModified() == 0) {
                toRemove.add(item);
            }
        }
        for (ClassStatistics itemToRemove : toRemove) {
            this.classStatistics.remove(itemToRemove);
        }
    }


    public String printEntitiesCreated() {
        //2 methods to prepare the class to be exploited
        removeClassStatisticsIfEmpty();
        pickUpIngestionTimes();

        StringBuilder builder = new StringBuilder("Entities created : %s. ".formatted(this.numberOfEntitiesInstanced));
        builder.append(""" 
                {
                    "report" : {
                    "number_of_entities_instanced" : %s,
                    "number_of_entities_modified" : %s,
                    "number_of_unchanged_entities" : %s,
                    "processing_time_ms" : %s ms,
                    "ingestion_time_ms": %s ms,
                """.formatted(this.numberOfEntitiesInstanced,
                this.numberOfEntitiesModified,
                this.numberOfUnchangedEntities,
                this.processingTime,
                this.ingestionTime));


        this.classStatistics.forEach(stats -> {
            builder.append("\"%s\": { %n".formatted(stats.getClassName()));
            if (stats.getEntitiesCreated() > 0) {
                builder.append("\t\"entities_created\": %s,%n".formatted(stats.getEntitiesCreated()));
            }
            if (stats.getEntitiesModified() > 0) {
                builder.append("\t\"entities_modified\": %s,%n".formatted(stats.getEntitiesModified()));
            }
            if (stats.getUnchangedEntities() > 0) {
                builder.append("\t\"unchanged_entities\": %s,%n".formatted(stats.getUnchangedEntities()));
            }
            //not yet implemented
            builder.append("\t\"processing_duration_ms\": %s ms,%n".formatted(stats.getProcessingTimeMs()))
                    .append("\t\"ingestion_duration_ms\": %s ms,%n".formatted(stats.getIngestionTimeMs()))
                    .append("},");


        });
        //remove last comma
        builder.deleteCharAt(builder.length() - 1)
                .append("\n } \n}");

        return builder.toString();
    }

    /**
     * Private Class used to serialize the stats data into Json
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @Getter
    private static class StatsToJson{

        private final int numberOfEntitiesInstanced;

        private final int numberOfEntitiesModified;
        private final int numberOfUnchangedEntities;


        private final long processingTimeMs;
        private final long ingestionTimeMs;
        private final List<ClassStatistics> classStatistics;

        public StatsToJson(EntityStatistics statistics) {
            this.numberOfEntitiesInstanced = statistics.numberOfEntitiesInstanced;
            this.numberOfEntitiesModified = statistics.numberOfEntitiesModified;
            this.numberOfUnchangedEntities = statistics.numberOfUnchangedEntities;
            this.classStatistics = statistics.classStatistics;
            this.processingTimeMs = statistics.processingTime;
            this.ingestionTimeMs = statistics.ingestionTime;
        }
    }

    public String getAsJson() throws JsonProcessingException {
        //2 methods to prepare the class to be exploited
        removeClassStatisticsIfEmpty();
        pickUpIngestionTimes();

        ObjectMapper mapper = JsonMapper.builder()
                .build();

        return mapper.writeValueAsString(new StatsToJson(getInstance()));
    }


    public long getProcessingTime() {
        return this.processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public long getIngestionTime() {
        return this.ingestionTime;
    }

    public void setIngestionTime(long ingestionTime) {
        this.ingestionTime = ingestionTime;
    }
}


