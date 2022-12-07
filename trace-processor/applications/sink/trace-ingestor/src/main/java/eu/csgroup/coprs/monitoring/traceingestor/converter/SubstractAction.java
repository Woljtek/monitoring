package eu.csgroup.coprs.monitoring.traceingestor.converter;

import java.time.Instant;
import java.util.*;

/**
 * Class that handle the subtract action (two value or more can be set).<br>
 * <br>
 * Order and definition of required arguments is the following:
 * <ul>
 *  <li>DYNAMIC: reference value</li>
 *  <li>DYNAMIC: value to subtract to the reference value</li>
 * </ul>
 *
 * Result type of the operation is of type of the first argument.<br>
 * <br>
 * Action support the following reference type and for each one compatible type are:
 * <ul>
 *     <li>{@link Instant}:</li>
 *      <ul>
 *          <li>{@link Instant}</li>
 *          <li>{@link Long} which is considered as a duration in seconds</li>
 *          <li>{@link Integer} which is considered as a duration in seconds</li>
 *          <li>{@link Double} which is considered as a duration in seconds (float part can have a precision in nanoseconds)</li>
 *      </ul>
 *     <li>{@link Integer}:</li>
 *      <ul>
 *          <li>{@link Long}</li>
 *          <li>{@link Integer}</li>
 *          <li>{@link Double} which is cast as an int value</li>
 *          <li>{@link Instant} which is considered as a value in milliseconds</li>
 *      </ul>
 *     <li>{@link Long}:</li>
 *      <ul>
 *          <li>{@link Long}</li>
 *          <li>{@link Integer}</li>
 *          <li>{@link Double} which is cast as a long value</li>
 *          <li>{@link Instant} which is considered as a value in milliseconds</li>
 *      </ul>
 *     <li>{@link Double}:</li>
 *      <ul>
 *          <li>{@link Long}</li>
 *          <li>{@link Integer}</li>
 *          <li>{@link Double}</li>
 *          <li>{@link Instant} which is considered as a value in milliseconds</li>
 *      </ul>
 * </ul>
 */
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
