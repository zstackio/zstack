package org.zstack.core.gc;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 8/5/2015.
 */
public class GCContext<T> {
    private Class runnerClass;
    private Class<T> contextClass;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private long interval;
    private T context;

    public GCContext() {
    }

    public GCContext(GCContext other) {
        this.runnerClass = other.runnerClass;
        this.contextClass = other.contextClass;
        this.timeUnit = other.timeUnit;
        this.interval = other.interval;
        this.context = (T) other.context;
    }


    public Class getRunnerClass() {
        return runnerClass;
    }

    public void setRunnerClass(Class runnerClass) {
        this.runnerClass = runnerClass;
    }

    public Class<T> getContextClass() {
        return contextClass;
    }

    public void setContextClass(Class<T> contextClass) {
        this.contextClass = contextClass;
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

    GCContextInternal toInternal() {
        GCContextInternal i = new GCContextInternal();
        if (contextClass != null) {
            i.contextClassName = contextClass.getName();
        }
        if (context != null) {
            i.context = JSONObjectUtil.rehashObject(map(e("context", context)), LinkedHashMap.class);
        }
        i.runnerClassName = runnerClass.getName();
        i.interval = interval;
        i.timeUnit = timeUnit;
        return i;
    }
}
