package eu.csgroup.coprs.monitoring.common.json;

public interface PropertyNames {
    String HEADER = "header";
    String MESSAGE = "message";
    String TASK = "task";
    String CUSTOM = "custom";
    String KUBERNETES = "kubernetes";

    String NAME = "name";

    // HEADER
    String TYPE = "type";
    String TIMESTAMP = "timestamp";
    String LEVEL = "level";
    String MISSION = "mission";
    String WORKFLOW = "workflow";
    String DEBUG_MODE = "debug_mode";
    String TAG_LIST = "tag_list";

    // MESSAGE
    String CONTENT = "content";

    // TASK
    String UID = "uid";
    String EVENT = "event";
    String DATA_RATE_MEBIBYTES_SEC = "data_rate_mebibytes_sec";
    String DATA_VOLUME_MEBIBYTES = "data_volume_mebibytes";
    String SATELLITE = "satellite";

    String INPUT = "input";

    String CHILD_OF_TASK = "child_of_task";
    String FOLLOWS_FROM_TASK = "follows_from_task";

    String STATUS = "status";
    String ERROR_CODE = "error_code";
    String DURATION_IN_SECONDS = "duration_in_seconds";
    String OUTPUT = "output";
    String QUALITY = "quality";

    // KUBERNETES
    String POD_NAME = "pod_name";
    String NAMESPACE_NAME = "namespace_name";
    String POD_ID = "pod_id";
    String LABELS = "labels";
    String POD_TEMPLATE_HASH = "pod-template-hash";
    String ANNOTATIONS = "annotations";
    String KUBERNETES_IO_CREATED_BY = "kubernetes_io/created_by";
    String HOST = "host";
    String CONTAINER_NAME = "container_name";
    String DOCKER_ID = "docker_id";

    String UID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    String VERSION_REGEX = "[0-9]*\\.[0-9]*\\.[0-9]*-[a-zA-Z0-9]*";

    String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
    String DEFAULT_TIMEZONE= "UTC";
    String TRACE_LOG_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'";
    int STRING_FIELD_256_LIMIT = 256;
    int STRING_FIELD_10K_LIMIT = 10000;
}
