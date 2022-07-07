package eu.csgroup.coprs.monitoring.tracefilter.json;

import javax.validation.ConstraintViolation;
import java.util.Set;

public class JsonValidationException extends Exception {
    private String violationMessage;

    public JsonValidationException() {
        super();
    }

    public JsonValidationException(String message) {
        super(message);
    }

    public JsonValidationException(Throwable throwable) {
        super(throwable);
    }

    public JsonValidationException(String message, Throwable throwable) {
        super(message);
    }

    @Override
    public String getLocalizedMessage() {
        String initialMessage  =  super.getLocalizedMessage();
        if (initialMessage == null || initialMessage.isEmpty()) {
            return  "Json object does not validate all constraints\n" + violationMessage;
        } else {
           return  initialMessage + "\n" + violationMessage;
        }
    }

    @Override
    public String getMessage() {
        String initialMessage  =  super.getMessage();
        if (initialMessage == null || initialMessage.isEmpty()) {
            return  "Json object does not validate all constraints\n" + violationMessage;
        } else {
            return  initialMessage + "\n" + violationMessage;
        }
    }

    public <T> void setViolations(Set<ConstraintViolation<T>> violations) {
        violationMessage = "";
        for (ConstraintViolation<?> violation : violations) {
            if (! violationMessage.isEmpty()) {
                violationMessage += "\n";
            }
            violationMessage += violation.getRootBeanClass().getSimpleName() + "." + violation.getPropertyPath() + " " + violation.getMessage();
        }
    }
}
