package eu.csgroup.coprs.monitoring.traceingestor.entity;

import eu.csgroup.coprs.monitoring.common.datamodel.entities.*;
import eu.csgroup.coprs.monitoring.common.ingestor.DataBaseIngestionTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;


public class EntityStatistics {

    private static int numberOfEntitiesInstanced;
    private static int numberOfEntitiesModified;
    private static int numberOfUnchangedEntities;

    private static List<ClassStatistics> classStatistics;
    private static long processingTime;

    //
    private static Class<? extends DefaultEntity> currentClass;

    static {
        reset();
    }

    private EntityStatistics() {
    }

    public static void reset() {
        numberOfEntitiesInstanced = 0;
        numberOfEntitiesModified = 0;
        numberOfUnchangedEntities = 0;
        classStatistics = new ArrayList<>();
        classStatistics.add(new ClassStatistics(AuxData.class));
        classStatistics.add(new ClassStatistics(Chunk.class));
        classStatistics.add(new ClassStatistics(Dsib.class));
        classStatistics.add(new ClassStatistics(Product.class));
        classStatistics.add(new ClassStatistics(Processing.class));
        classStatistics.add(new ClassStatistics(InputListExternal.class));
        classStatistics.add(new ClassStatistics(InputListInternal.class));
        classStatistics.add(new ClassStatistics(OutputList.class));
        classStatistics.add(new ClassStatistics(MissingProducts.class));

    }

    //old way of doing it, keep it just in case
    public static void incorporateEntitiesList(List<EntityProcessing> processedEntities) {
        //group by DefaultEntity
        processedEntities.stream()
                .collect(groupingBy(EntityProcessing::getEntity))
                .forEach((defaultEntity, entites) -> {
                    //set the currentClasstoIncrement
                    EntityStatistics.setCurrentClass(defaultEntity.getClass());
                    //increment global amount, and the currentClass
                    for (EntityProcessing entity : entites) {
                        if (entity.getState().equals(EntityState.NEW)) {
                            EntityStatistics.incrementNumberOfEntitiesInstanced(1);
                        } else if (entity.getState().equals(EntityState.UPDATED)) {
                            EntityStatistics.incrementNumberOfEntitiesModified(1);
                        } else if (entity.getState().equals(EntityState.UNCHANGED)) {
                            EntityStatistics.incrementNumberOfUnchangedEntities(1);
                        }
                    }
                });
    }

    public static void incorporateEntities(Map<String, List<EntityProcessing>> processedEntities) {
        //from a map to a list
        processedEntities.entrySet()
                .stream().flatMap(entry -> entry.getValue().stream())
                //group by DefaultEntity
                .collect(groupingBy(EntityProcessing::getEntity))
                .forEach((defaultEntity, entites) -> {
                    //set the currentClasstoIncrement
                    EntityStatistics.setCurrentClass(defaultEntity.getClass());
                    //increment global amount, and the currentClass
                    for (EntityProcessing entity : entites) {
                        if (entity.getState().equals(EntityState.NEW)) {
                            EntityStatistics.incrementNumberOfEntitiesInstanced(1);
                        } else if (entity.getState().equals(EntityState.UPDATED)) {
                            EntityStatistics.incrementNumberOfEntitiesModified(1);
                        } else if (entity.getState().equals(EntityState.UNCHANGED)) {
                            EntityStatistics.incrementNumberOfUnchangedEntities(1);
                        }
                    }
                });
    }

    public static void retrieveProcessingTime(long milliSeconds, Class<? extends DefaultEntity> entityClass) {
        //no need to check for the presence of the Entry, I literally create them at class loading
        getClassStatisticsByClass(entityClass).get().setProcessingTime(milliSeconds);

    }

    public static void setCurrentClass(Class<? extends DefaultEntity> current) {
        currentClass = current;
    }


    public static Optional<ClassStatistics> getClassStatistics(DefaultEntity entity) {
        return EntityStatistics.classStatistics.stream()
                .filter(classStats -> classStats.getEntityClass().equals(entity.getClass()))
                .findAny();
    }

    private static Optional<ClassStatistics> getClassStatisticsByClass(Class<? extends DefaultEntity> currentClass) {
        return EntityStatistics.classStatistics.stream()
                .filter(stat -> stat.getEntityClass().equals(currentClass))
                .findFirst();
    }

    public static void incrementNumberOfEntitiesInstanced(int number) {
        EntityStatistics.numberOfEntitiesInstanced += number;

        Optional<ClassStatistics> currentOptional = getClassStatisticsByClass(EntityStatistics.currentClass);
        if (currentOptional.isPresent()) {
            var current = currentOptional.get();
            current.incrementEntitiesCreated(number);
        }
    }

    public static void incrementNumberOfUnchangedEntities(int number) {
        EntityStatistics.numberOfUnchangedEntities += number;

        Optional<ClassStatistics> currentOptional = getClassStatisticsByClass(EntityStatistics.currentClass);
        if (currentOptional.isPresent()) {
            var current = currentOptional.get();
            current.incrementUnchangedEntities(number);
        }
    }


    public static void incrementNumberOfEntitiesModified(int number) {
        EntityStatistics.numberOfEntitiesModified += number;

        Optional<ClassStatistics> currentOptional = getClassStatisticsByClass(EntityStatistics.currentClass);
        if (currentOptional.isPresent()) {
            var current = currentOptional.get();
            current.incrementEntitiesModified(number);
        }
    }


    private static void removeClassStatisticsIfEmpty() {
        List<ClassStatistics> toRemove = new ArrayList<>();
        for (ClassStatistics item : EntityStatistics.classStatistics) {
            if (item.getEntitiesCreated() == 0 && item.getEntitiesModified() == 0) {
                toRemove.add(item);
            }
        }
        for (ClassStatistics itemToRemove : toRemove) {
            EntityStatistics.classStatistics.remove(itemToRemove);
        }
    }


    public static String printEntitiesCreated() {
        //getting the databaseStorage Times from the common project
        //since this method is only called after the processing and saving has been done, we know the fields are filled!
        DataBaseIngestionTimer dataStorageTimes = DataBaseIngestionTimer.getInstance();

        StringBuilder builder = new StringBuilder("Entities created : %s. ".formatted(numberOfEntitiesInstanced));
        builder.append(""" 
                {
                    "report" : {
                    "entites_created" : %s,
                    "entities_modified" : %s,
                    "entities_unchanged" : %s,
                    "processing_duration" : %s,
                    "ingestion_duration": %s,
                """.formatted(numberOfEntitiesInstanced,
                numberOfEntitiesModified,
                numberOfUnchangedEntities,
                processingTime,
                dataStorageTimes.resolveGlobalTimer()));

        EntityStatistics.removeClassStatisticsIfEmpty();

        EntityStatistics.classStatistics.forEach(stats -> {
            builder.append("\"%s\": { \n".formatted(stats.getClassName()));
            if (stats.getEntitiesCreated() > 0) {
                builder.append("\"created\": %s,\n".formatted(stats.getEntitiesCreated()));
            }
            if (stats.getEntitiesModified() > 0) {
                builder.append("\"updated\": %s,\n".formatted(stats.getEntitiesModified()));
            }
            if (stats.getUnchangedEntities() > 0) {
                builder.append("\"unchanged\": %s,\n".formatted(stats.getUnchangedEntities()));
            }
            //not yet implemented
            builder.append("\"processing_duration\": %s,\n".formatted(stats.getProcessingTime()))
                    .append("\"ingestion_duration\": %s,\n".formatted(dataStorageTimes.resolveUnitaryTimer(stats.getEntityClass())))
                    .append("},");


        });
        //remove last comma
        builder.deleteCharAt(builder.length() - 1)
                .append("\n } \n}");

        //maybe call reset() here?
        //maybe not, we might want to get the data in Json form !
        return builder.toString();
    }


    public static long getProcessingTime() {
        return processingTime;
    }

    public static void setProcessingTime(long processingTime) {
        EntityStatistics.processingTime = processingTime;
    }

}


