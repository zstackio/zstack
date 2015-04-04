package org.zstack.core.thread;

import java.util.concurrent.TimeUnit;

public interface PeriodicTask extends Runnable {
    TimeUnit getTimeUnit();
    
    long getInterval();
    
    String getName();
}
