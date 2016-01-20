package org.zstack.core.gc;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 11/10/2015.
 */
public class AbstractGCContext<T> implements GCContext {
    protected TimeUnit timeUnit = TimeUnit.SECONDS;
    protected long interval;
    protected T context;
    protected String name;
    protected long executedTimes;

    public AbstractGCContext() {
    }

    public AbstractGCContext(AbstractGCContext<T> other) {
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

    public T getContext() {
        return context;
    }

    public void setContext(T context) {
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getExecutedTimes() {
        return executedTimes;
    }

    public void setExecutedTimes(long executedTimes) {
        this.executedTimes = executedTimes;
    }

    public long increaseExecutedTime() {
        return ++ executedTimes;
    }
}
