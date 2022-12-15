package eu.csgroup.coprs.monitoring.common.datamodel;

public final class Properties {
    public static final String UID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    public static final String VERSION_REGEX = "[0-9]*\\.[0-9]*\\.[0-9]*-?[a-zA-Z0-9]*";

    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
    public static final String DEFAULT_TIMEZONE= "UTC";
    public static final String TRACE_LOG_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'";
    public static final int STRING_FIELD_256_LIMIT = 256;
    public static final int STRING_FIELD_10K_LIMIT = 10000;
}
