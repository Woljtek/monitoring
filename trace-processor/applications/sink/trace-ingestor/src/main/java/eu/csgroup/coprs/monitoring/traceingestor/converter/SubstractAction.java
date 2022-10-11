package eu.csgroup.coprs.monitoring.traceingestor.converter;

import java.time.Instant;
import java.util.*;

public class SubstractAction extends Action {

    public SubstractAction(String rawAction) {
        super(rawAction);
    }

    @Override
    protected List<ARG_TYPE> getArgsMapping() {
        return List.of(ARG_TYPE.DYNAMIC);
    }

    @Override
    protected String getRequiredAction() {
        return ActionConstant.SUBSTRACT_ACTION_PATTERN;
    }

    @Override
    public Object execute(List<Object> values) {
        final var valuesIt = values.iterator();
        final var ref = valuesIt.next();

        if (ref instanceof Instant date) {
            return subtractDate(date, valuesIt);
        } else if (ref instanceof Integer castVal) {
            return subtractInt(castVal, valuesIt);
        } else if (ref instanceof Long castVal) {
            return subtractLong(castVal, valuesIt);
        } else if (ref instanceof Double castVal) {
            return subtractDouble(castVal, valuesIt);
        }

        return null;
    }

    private Instant subtractDate(Instant refDate, Iterator<Object> valuesIt) {
        var currentDate = refDate;
        while (valuesIt.hasNext()) {
            var nextVal = valuesIt.next();
            if (nextVal instanceof Long castVal) {
                currentDate = currentDate.minusSeconds(castVal);
            } else if (nextVal instanceof Integer castVal) {
                currentDate = currentDate.minusSeconds(castVal);
            } else if (nextVal instanceof Double castVal) {
                // Expect value to be in second but handle micro second precision
                currentDate = currentDate.minusNanos((long)(castVal * 1000 * 1000 * 1000));
            } else if (nextVal instanceof Instant castVal) {
                currentDate = currentDate.minusMillis(castVal.toEpochMilli());
            }
        }

        return currentDate;
    }

    private int subtractInt (int refValue, Iterator<Object> valuesIt) {
        var currentValue = refValue;
        while (valuesIt.hasNext()) {
            var nextVal = valuesIt.next();
            if (nextVal instanceof Long castVal) {
                currentValue = (int) (currentValue - castVal);
            } else if (nextVal instanceof Integer castVal) {
                currentValue = currentValue - castVal;
            } else if (nextVal instanceof Double castVal) {
                currentValue = currentValue - castVal.intValue();
            } else if (nextVal instanceof Instant castVal) {
                currentValue = (int) (currentValue - castVal.toEpochMilli());
            }
        }

        return currentValue;
    }

    private long subtractLong (long refValue, Iterator<Object> valuesIt) {
        var currentValue = refValue;
        while (valuesIt.hasNext()) {
            var nextVal = valuesIt.next();
            if (nextVal instanceof Long castVal) {
                currentValue = currentValue - castVal;
            } else if (nextVal instanceof Integer castVal) {
                currentValue = currentValue - castVal;
            } else if (nextVal instanceof Double castVal) {
                currentValue = currentValue - castVal.longValue();
            } else if (nextVal instanceof Instant castVal) {
                currentValue = currentValue - castVal.toEpochMilli();
            }
        }

        return currentValue;
    }

    private double subtractDouble (double refValue, Iterator<Object> valuesIt) {
        var currentValue = refValue;
        while (valuesIt.hasNext()) {
            var nextVal = valuesIt.next();
            if (nextVal instanceof Long castVal) {
                currentValue = currentValue - castVal;
            } else if (nextVal instanceof Integer castVal) {
                currentValue = currentValue - castVal;
            } else if (nextVal instanceof Double castVal) {
                currentValue = currentValue - castVal;
            } else if (nextVal instanceof Instant castVal) {
                currentValue = currentValue - castVal.toEpochMilli();
            }
        }

        return currentValue;
    }

}
