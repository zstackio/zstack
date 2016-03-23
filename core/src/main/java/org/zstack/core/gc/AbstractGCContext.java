package org.zstack.core.gc;

/**
 * Created by xing5 on 2016/3/22.
 */
public abstract class AbstractGCContext<T> implements GCContext<T> {
    protected T context;
    protected String name;
    protected long executedTimes;

    public AbstractGCContext() {
    }

    public AbstractGCContext(AbstractGCContext<T> other) {
        this.context = other.context;
        this.name = other.name;
        this.executedTimes = other.executedTimes;
    }

    @Override
    public T getContext() {
        return context;
    }

    public void setContext(T context) {
        this.context = context;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
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
