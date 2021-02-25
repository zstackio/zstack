package org.zstack.core.thread;

import org.zstack.header.Component;
import org.zstack.header.core.progress.ChainInfo;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface ThreadFacade extends Component {
    <T> Future<T> submit(Task<T> task);

    <T> Future<T> submitSyncPool(Task<T> task);

    <T> Future<T> syncSubmit(SyncTask<T> task);
    
    Future<Void> chainSubmit(ChainTask task);

    boolean isChainTaskRunning(String signature);

    ChainInfo getChainTaskInfo(String signature);

    Set<String> getApiRunningTaskSignature(String apiId);

    Future<Void> submitPeriodicTask(PeriodicTask task, long delay);
    
    Future<Void> submitPeriodicTask(PeriodicTask task);
    
    Future<Void> submitCancelablePeriodicTask(CancelablePeriodicTask task);
    
    Future<Void> submitCancelablePeriodicTask(CancelablePeriodicTask task, long delay);
    
    void registerHook(ThreadAroundHook hook); 
    
    void unregisterHook(ThreadAroundHook hook);
    
    ThreadFacadeImpl.TimeoutTaskReceipt submitTimeoutTask(Runnable task, TimeUnit unit, long delay);

    ThreadFacadeImpl.TimeoutTaskReceipt submitTimeoutTask(Runnable task, TimeUnit unit, long delay, boolean executeRightNow);

    Runnable submitTimerTask(TimerTask task, TimeUnit unit, long delay);

    int getSyncThreadNum(int totalThreadNum);
    void printThreadsAndTasks();
}
