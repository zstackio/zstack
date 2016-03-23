package org.zstack.core.gc;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 11/10/2015.
 */
public class AbstractTimeBasedGCContext<T> extends AbstractGCContext<T> implements TimeBasedGCContext<T> {
    protected TimeUnit timeUnit = TimeUnit.SECONDS;
    protected long interval;


    public AbstractTimeBasedGCContext() {
    }

    public AbstractTimeBasedGCContext(AbstractTimeBasedGCContext<T> other) {
        this.timeUnit = other.timeUnit;
        this.interval = other.interval;
        this.context = other.context;
        this.name = other.name;
        this.executedTimes = other.executedTimes;
    }


    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }
}
