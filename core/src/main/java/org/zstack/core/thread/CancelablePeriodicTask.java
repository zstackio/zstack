package org.zstack.core.thread;

import org.zstack.header.HasThreadContext;

import java.util.concurrent.TimeUnit;

public interface CancelablePeriodicTask extends HasThreadContext {
	boolean run();
	
    TimeUnit getTimeUnit();
    
    long getInterval();
    
    String getName();
}
