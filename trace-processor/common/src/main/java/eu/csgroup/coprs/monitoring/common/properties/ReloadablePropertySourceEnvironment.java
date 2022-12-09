package eu.csgroup.coprs.monitoring.common.properties;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

/**
 * Singleton instance that manipulate global properties to define refresh period for configuration files
 */
@Getter
@Setter
public class ReloadablePropertySourceEnvironment {
    public static final long DEFAULT_REFRESH_PERIOD = 1;

    public static final TimeUnit DEFAULT_REFRESH_PERIOD_UNIT = TimeUnit.MINUTES;

    private static final ReloadablePropertySourceEnvironment INSTANCE = new ReloadablePropertySourceEnvironment();

    private long refreshPeriodValue = DEFAULT_REFRESH_PERIOD;

    private TimeUnit refreshPeriodUnit = DEFAULT_REFRESH_PERIOD_UNIT;

    private ReloadablePropertySourceEnvironment () {

    }

    public static ReloadablePropertySourceEnvironment getInstance () {
        return INSTANCE;
    }

    /**
     * Change global refresh period
     *
     * @param refreshPeriod period
     * @param timeUnit unit time
     */
    public void setRefreshPeriod(long refreshPeriod, TimeUnit timeUnit) {
        this.refreshPeriodValue = refreshPeriod;
        this.refreshPeriodUnit = timeUnit;
    }
}
