package org.zstack.core.thread;

import java.util.Map;
import java.util.concurrent.Future;

public interface DispatchQueue {
    <T> Future<T> syncSubmit(SyncTask<T> task);
    
    Future<Void> chainSubmit(ChainTask task);

    Map<String, SyncTaskStatistic> getSyncTaskStatistics();

    Map<String, ChainTaskStatistic> getChainTaskStatistics();
}
