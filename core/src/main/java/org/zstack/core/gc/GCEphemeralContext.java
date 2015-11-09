package org.zstack.core.gc;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 11/9/2015.
 */
public class GCEphemeralContext<T> implements GCContext {
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private long interval;
    private T context;
    private GCRunner runner;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public GCRunner getRunner() {
        return runner;
    }

    public void setRunner(GCRunner runner) {
        this.runner = runner;
    }
}
