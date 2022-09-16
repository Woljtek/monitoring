package eu.csgroup.coprs.monitoring.traceingestor.mapper;

public class InterruptedOperationException extends RuntimeException {
    public InterruptedOperationException(String message) {
        super(message);
    }
}
