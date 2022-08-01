package eu.csgroup.coprs.monitoring.traceingestor.entity;

public class EntityHandlerException extends RuntimeException {
    public EntityHandlerException () {
        super();
    }

    public EntityHandlerException (String message) {
        super(message);
    }

    public EntityHandlerException (Throwable origin) {
        super(origin);
    }

    public EntityHandlerException (String message, Throwable origin) {
        super(message, origin);
    }
}
