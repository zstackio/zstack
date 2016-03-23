package org.zstack.core.gc;

/**
 * Created by frank on 11/9/2015.
 */
public interface GCContext<T> {
    T getContext();
    String getName();
    long getExecutedTimes();
}
