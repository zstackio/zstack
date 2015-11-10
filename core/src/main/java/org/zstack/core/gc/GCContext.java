package org.zstack.core.gc;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 11/9/2015.
 */
public interface GCContext<T> {
    T getContext();
    TimeUnit getTimeUnit();
    long getInterval();
    String getName();
    long getExecutedTimes();
}
