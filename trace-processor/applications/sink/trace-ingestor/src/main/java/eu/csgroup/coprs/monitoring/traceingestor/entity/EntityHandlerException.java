package eu.csgroup.coprs.monitoring.traceingestor.entity;

import java.io.Serial;

public class EntityHandlerException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -5191711481626759514L;

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
