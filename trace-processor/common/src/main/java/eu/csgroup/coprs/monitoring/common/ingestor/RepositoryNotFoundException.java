package eu.csgroup.coprs.monitoring.common.ingestor;

import java.io.Serial;

public class RepositoryNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -4349173471306470028L;

    public RepositoryNotFoundException (String message) {
        super(message);
    }
}
