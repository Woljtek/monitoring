package eu.csgroup.coprs.monitoring.traceingestor.mapper;

import java.io.Serial;

public class InterruptedOperationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -2203129201027853275L;

    public InterruptedOperationException(String message) {
        super(message);
    }
}
