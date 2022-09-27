package eu.csgroup.coprs.monitoring.common.ingestor;

import java.io.Serial;

public class EntityException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6712281652173065041L;

    public EntityException(String message, Throwable e) {
        super(message, e);
    }
}
