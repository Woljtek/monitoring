package eu.csgroup.coprs.monitoring.traceingestor.association;

import java.io.Serial;

public class AssociationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 3980750517927123585L;

    public AssociationException(String message, Throwable e) {
        super(message, e);
    }
}
