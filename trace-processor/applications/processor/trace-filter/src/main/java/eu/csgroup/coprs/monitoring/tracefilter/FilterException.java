package eu.csgroup.coprs.monitoring.tracefilter;

public class FilterException extends RuntimeException {
    public FilterException (String message, Throwable e) {
        super(message, e);
    }
}
