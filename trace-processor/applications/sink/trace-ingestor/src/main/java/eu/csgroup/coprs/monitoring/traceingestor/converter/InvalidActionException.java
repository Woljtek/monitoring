package eu.csgroup.coprs.monitoring.traceingestor.converter;

public class InvalidActionException extends RuntimeException {
    public InvalidActionException (String message) {
        super(message);
    }
}
