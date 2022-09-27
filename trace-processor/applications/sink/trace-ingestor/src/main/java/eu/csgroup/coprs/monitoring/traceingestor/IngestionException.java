package eu.csgroup.coprs.monitoring.traceingestor;

import java.io.Serial;

public class IngestionException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 7679275960759406013L;

    public IngestionException(String message, Throwable e) {
        super(message, e);
    }

    public IngestionException(String message) {
        super(message);
    }
}
