package org.zstack.core.thread;

import org.zstack.header.Component;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface ThreadFacade extends Component {
    <T> Future<T> submit(Task<T> task);
    
    <T> Future<T> syncSubmit(SyncTask<T> task);
    
    Future<Void> chainSubmit(ChainTask task);
    
    Future<Void> submitPeriodicTask(PeriodicTask task, long delay);
    
    Future<Void> submitPeriodicTask(PeriodicTask task);
    
    Future<Void> submitCancelablePeriodicTask(CancelablePeriodicTask task);
    
    Future<Void> submitCancelablePeriodicTask(CancelablePeriodicTask task, long delay);
    
    void registerHook(ThreadAroundHook hook); 
    
    void unregisterHook(ThreadAroundHook hook);
    
    ThreadFacadeImpl.TimeoutTaskReceipt submitTimeoutTask(Runnable task, TimeUnit unit, long delay);

    void submitTimerTask(TimerTask task, TimeUnit unit, long delay);
}
