package org.zstack.core.gc;

import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/3/22.
 */
public interface TimeBasedGCContext<T> extends GCContext<T> {
    TimeUnit getTimeUnit();
    long getInterval();
}
