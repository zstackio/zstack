package org.zstack.core.thread;

import java.util.concurrent.TimeUnit;

public interface CancelablePeriodicTask {
	boolean run();
	
    TimeUnit getTimeUnit();
    
    long getInterval();
    
    String getName();
}
