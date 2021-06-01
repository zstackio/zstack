package org.zstack.core.thread;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.debug.DebugManager;
import org.zstack.core.debug.DebugSignalHandler;
import org.zstack.header.Constants;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.core.progress.PendingTaskInfo;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TaskContext;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
class DispatchQueueImpl implements DispatchQueue, DebugSignalHandler {
    private static final CLogger logger = Utils.getLogger(DispatchQueueImpl.class);

    @Autowired
    ThreadFacade _threadFacade;
    @Autowired
    private org.zstack.core.timeout.Timer zTimer;

    private final HashMap<String, SyncTaskQueueWrapper> syncTasks = new HashMap<String, SyncTaskQueueWrapper>();
    private final Map<String, ChainTaskQueueWrapper> chainTasks = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, List<String>> apiRunningSignature = new ConcurrentHashMap<>();
    private static final CLogger _logger = CLoggerImpl.getLogger(DispatchQueueImpl.class);

    @Override
    public void handleDebugSignal() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================= BEGIN TASK QUEUE DUMP ================");
        sb.append("\nASYNC TASK QUEUE DUMP:");
        sb.append(String.format("\nTASK QUEUE NUMBER: %s\n", chainTasks.size()));
        List<String> asyncTasks = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (chainTasks) {
            for (Map.Entry<String, ChainTaskQueueWrapper> e : chainTasks.entrySet()) {
                StringBuilder tb = new StringBuilder(String.format("\nQUEUE SYNC SIGNATURE: %s", e.getKey()));
                ChainTaskQueueWrapper w = e.getValue();
                tb.append(String.format("\nRUNNING TASK NUMBER: %s", w.runningQueue.size()));
                tb.append(String.format("\nPENDING TASK NUMBER: %s", w.pendingQueue.size()));
                tb.append(String.format("\nASYNC LEVEL: %s", w.maxThreadNum));

                int index = 0;
                for (Object obj : w.runningQueue) {
                    ChainFuture cf = (ChainFuture) obj;
                    tb.append(TaskInfoBuilder.buildRunningTaskInfo(cf, now, index++));
                }

                for (Object obj : w.pendingQueue) {
                    ChainFuture cf = (ChainFuture) obj;
                    tb.append(TaskInfoBuilder.buildPendingTaskInfo(cf, now, index++));
                }
                asyncTasks.add(tb.toString());
            }
        }

        sb.append(StringUtils.join(asyncTasks, "\n"));
        sb.append("\n================= END TASK QUEUE DUMP ==================\n");
        _threadFacade.printThreadsAndTasks();
        logger.debug(sb.toString());
    }

    public void beforeCleanQueuedumpThread(String signatureName) {
        String title = "\n================= Before Clean Task Queue Dump ================";
        dumpsignatureNameThread(signatureName,title);
    }

    public void afterCleanQueuedumpThread(String signatureName) {
        String title = "\n================= After Clean Task Queue Dump ================";
        dumpsignatureNameThread(signatureName,title);
    }

    public void dumpsignatureNameThread(String signatureName,String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        sb.append("\nASYNC TASK QUEUE DUMP:");
        sb.append(String.format("\nTASK QUEUE NUMBER: %s\n", chainTasks.size()));
        List<String> asyncTasks = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (chainTasks) {
            ChainTaskQueueWrapper w = chainTasks.get(signatureName);
            if (w == null) {
                sb.append(String.format("\n===== NO QUEUE SYNC SIGNATURE: %s =====", signatureName));
                sb.append(StringUtils.join(asyncTasks, "\n"));
                sb.append("\n================= END TASK QUEUE DUMP ==================\n");
                _threadFacade.printThreadsAndTasks();
                logger.debug(sb.toString());
                return;
            }
            StringBuilder tb = new StringBuilder(String.format("\nQUEUE SYNC SIGNATURE: %s", signatureName));
            tb.append(String.format("\nRUNNING TASK NUMBER: %s", w.runningQueue.size()));
            tb.append(String.format("\nPENDING TASK NUMBER: %s", w.pendingQueue.size()));
            tb.append(String.format("\nASYNC LEVEL: %s", w.maxThreadNum));
            int index = 0;
            for (Object obj : w.runningQueue) {
                ChainFuture cf = (ChainFuture) obj;
                tb.append(TaskInfoBuilder.buildRunningTaskInfo(cf, now, index++));
            }

            for (Object obj : w.pendingQueue) {
                ChainFuture cf = (ChainFuture) obj;
                tb.append(TaskInfoBuilder.buildPendingTaskInfo(cf, now, index++));
            }
            asyncTasks.add(tb.toString());
        }

        sb.append(StringUtils.join(asyncTasks, "\n"));
        sb.append("\n================= END TASK QUEUE DUMP ==================\n");
        _threadFacade.printThreadsAndTasks();
        logger.debug(sb.toString());
    }

    @Override
    public ChainInfo getChainTaskInfo(String signature) {
        long now = System.currentTimeMillis();
        synchronized (chainTasks) {
            ChainInfo info = new ChainInfo();
            ChainTaskQueueWrapper w = chainTasks.get(signature);
            if (w == null) {
                return info;
            }

            int index = 0;
            for (Object obj : w.runningQueue) {
                ChainFuture cf = (ChainFuture) obj;
                info.addRunningTask(TaskInfoBuilder.buildRunningTaskInfo(cf, now, index++));
            }

            for (Object obj : w.pendingQueue) {
                ChainFuture cf = (ChainFuture) obj;
                info.addPendingTask(TaskInfoBuilder.buildPendingTaskInfo(cf, now, index++));
            }
            return info;
        }
    }

    @Override
    public ChainInfo cleanChainTaskInfo(String signature, Integer index, Boolean cleanUp, Boolean isRunningTask) {
        beforeCleanQueuedumpThread(signature);
        long now = System.currentTimeMillis();
        synchronized (chainTasks) {
            ChainInfo info = new ChainInfo();
            ChainTaskQueueWrapper w = chainTasks.get(signature);
            if (w == null) {
                logger.warn(String.format("no queue with a corresponding signatureName[%s]", signature));
                return null;
            }

            ChainInfo Tmp = getChainTaskInfo(signature);
            if (cleanUp) {
                chainTasks.remove(signature);
                afterCleanQueuedumpThread(signature);
                return Tmp;
            }
            if (index == null) {
                if (isRunningTask) {
                    info.setRunningTask(Tmp.getRunningTask());
                    w.runningQueue.clear();
                    afterCleanQueuedumpThread(signature);
                    return info;
                }
                info.setPendingTask(Tmp.getPendingTask());
                w.pendingQueue.clear();
                afterCleanQueuedumpThread(signature);
                return info;
            }

            if (isRunningTask) {
                ChainFuture cf = (ChainFuture) w.runningQueue.get(index);
                info.addRunningTask(TaskInfoBuilder.buildRunningTaskInfo(cf, now, index));
                w.runningQueue.remove(index.intValue());
            } else {
                ChainFuture cf = (ChainFuture) w.pendingQueue.get(index);
                info.addPendingTask(TaskInfoBuilder.buildPendingTaskInfo(cf, now, index));
                w.pendingQueue.remove(index.intValue());
            }

            if (w.runningQueue.isEmpty() && w.pendingQueue.isEmpty()) {
                chainTasks.remove(signature);
            }
            afterCleanQueuedumpThread(signature);       
            return info;
        }
    }

    @Override
    public Set<String> getApiRunningTaskSignature(String apiId) {
        return new HashSet<>(apiRunningSignature.getOrDefault(apiId, Collections.emptyList()));
    }

    public DispatchQueueImpl() {
        DebugManager.registerDebugSignalHandler(DUMP_TASK_DEBUG_SINGAL, this);
    }

    private class SyncTaskFuture<T> extends AbstractFuture<T> {
        public SyncTaskFuture(SyncTask<T> task) {
            super(task);
        }

        private SyncTask getTask() {
            return (SyncTask) task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return true;
        }

        void run() {
            if (isCancelled()) {
                return;
            }

            try {
                ret = (T) getTask().call();
            } catch (Throwable t) {
                _logger.warn(String.format("unhandled exception happened when calling sync task[name:%s, class:%s]",
                        getTask().getName(), getTask().getClass().getName()), t);
                exception = t;
            }

            done();
        }

        String getSyncSignature() {
            return getTask().getSyncSignature();
        }

        int getSyncLevel() {
            return getTask().getSyncLevel();
        }

        String getName() {
            return getTask().getName();
        }
    }

    private class SyncTaskQueueWrapper {
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
        AtomicInteger counter = new AtomicInteger(0);
        int maxThreadNum = -1;
        String syncSignature;

        void addTask(SyncTaskFuture task) {
            queue.offer(task);
            if (maxThreadNum == -1) {
                maxThreadNum = task.getSyncLevel();
            }
            if (syncSignature == null) {
                syncSignature = task.getSyncSignature();
            }
        }

        void startThreadIfNeeded() {
            if (counter.get() >= maxThreadNum) {
                return;
            }

            counter.incrementAndGet();
            _threadFacade.submitSyncPool(new Task<Void>() {
                @Override
                public String getName() {
                    return syncSignature;
                }

                void run() {
                    SyncTaskFuture stask;
                    while (true) {
                        while ((stask = (SyncTaskFuture) queue.poll()) != null) {
                            stask.run();
                        }

                        synchronized (syncTasks) {
                            if (queue.isEmpty()) {
                                if (counter.decrementAndGet() == 0) {
                                    syncTasks.remove(syncSignature);
                                }

                                break;
                            }
                        }
                    }

                }

                @Override
                public Void call() {
                    run();
                    return null;
                }
            });
        }
    }

    private <T> Future<T> doSyncSubmit(final SyncTask<T> syncTask) {
        assert syncTask.getSyncSignature() != null : "How can you submit a sync task without sync signature ???";

        SyncTaskFuture f;
        synchronized (syncTasks) {
            SyncTaskQueueWrapper wrapper = syncTasks.get(syncTask.getSyncSignature());
            if (wrapper == null) {
                wrapper = new SyncTaskQueueWrapper();
                syncTasks.put(syncTask.getSyncSignature(), wrapper);
            }
            f = new SyncTaskFuture(syncTask);
            wrapper.addTask(f);
            wrapper.startThreadIfNeeded();
        }

        return f;
    }

    @Override
    public <T> Future<T> syncSubmit(SyncTask<T> task) {
        if (task.getSyncLevel() <= 0) {
            return _threadFacade.submitSyncPool(task);
        } else {
            return doSyncSubmit(task);
        }
    }


    class ChainFuture extends AbstractFuture {
        private AtomicBoolean isNextCalled = new AtomicBoolean(false);

        private long startPendingTimeInMills = System.currentTimeMillis();
        private Long startExecutionTimeInMills;

        public long getStartPendingTimeInMills() {
            return startPendingTimeInMills;
        }

        public Long getStartExecutionTimeInMills() {
            return startExecutionTimeInMills;
        }

        public void setStartExecutionTimeInMills(Long startExecutionTimeInMills) {
            this.startExecutionTimeInMills = startExecutionTimeInMills;
        }

        public ChainFuture(ChainTask task) {
            super(task);
        }

        ChainTask getTask() {
            return (ChainTask) task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return true;
        }

        private void callNext(SyncTaskChain chain) {
            if (!isNextCalled.compareAndSet(false, true)) {
                return;
            }

            chain.next();
        }

        public void run(final SyncTaskChain chain) {
            if (isCancelled()) {
                callNext(chain);
                return;
            }

            try {
                getTask().run(() -> {
                    try {
                        done();
                    } finally {
                        callNext(chain);
                    }
                });
            } catch (Throwable t) {
                try {
                    if (!(t instanceof OperationFailureException)) {
                        _logger.warn(String.format("unhandled exception happened when calling %s", task.getClass().getName()), t);
                    }

                    done();
                } finally {
                    callNext(chain);
                }
            }
        }

        public int getSyncLevel() {
            return getTask().getSyncLevel();
        }

        public String getSyncSignature() {
            return getTask().getSyncSignature();
        }
    }

    private class ChainTaskQueueWrapper {
        LinkedList pendingQueue = new LinkedList();
        final Map<String, AtomicInteger> subPendingMap = new ConcurrentHashMap<>();
        final LinkedList runningQueue = new LinkedList();
        AtomicInteger counter = new AtomicInteger(0);
        int maxThreadNum = -1;
        String syncSignature;

        int addSubPending(String deduplicateStr) {
            subPendingMap.compute(deduplicateStr, (k, v) -> {
                if (v == null) {
                    return new AtomicInteger(1);
                } else {
                    v.incrementAndGet();
                    return v;
                }
            });
            return subPendingMap.get(deduplicateStr).intValue();
        }

        void removeSubPending(String deduplicateStr, boolean removeIfZero) {
            subPendingMap.computeIfPresent(deduplicateStr, (k, v) -> {
                int r = v.decrementAndGet();
                if (r < 0) {
                    if (removeIfZero) {
                        return null;
                    }
                }
                return v;
            });
        }

        void removeSubPendingZero(String deduplicateStr) {
            subPendingMap.computeIfPresent(deduplicateStr, (k, v) -> {
                int r = v.intValue();
                if (r == 0) {
                    return null;
                }
                return v;
            });
        }

        void warningAndRemove(ChainFuture task, int length, int queueLength) {
            logger.warn(String.format("[%s] max pending size: %d, pending now: %d, throw the task: %s!", task.getTask().getDeduplicateString(), length, queueLength, task.getTask().getName()));
            removeSubPending(task.getTask().getDeduplicateString(), true);
        }

        boolean addTask(ChainFuture task, int length) {
            if (length != -1 && CoreGlobalProperty.CHAIN_TASK_QOS) {
                DebugUtils.Assert(task.getTask().getDeduplicateString() != null, "deduplicate String must be set if max pending string has been set!");
                AtomicInteger r = subPendingMap.get(task.getTask().getDeduplicateString());
                int queueLength = addSubPending(task.getTask().getDeduplicateString());
                if (queueLength > length) {
                    synchronized (runningQueue) {
                        if (length != 0 || queueLength != 1 || r != null) {
                            warningAndRemove(task, length, queueLength);
                            return false;
                        }
                    }
                }
            }
            pendingQueue.offer(task);

            if (maxThreadNum == -1) {
                maxThreadNum = task.getSyncLevel();
            } else if (maxThreadNum < task.getSyncLevel()) {
                logger.warn(String.format("task[name:%s] increases queue[name:%s]'s sync level from %s to %s", task.getTask().getName(), task.getSyncSignature(), maxThreadNum, task.getSyncLevel()));
                maxThreadNum = task.getSyncLevel();
            }

            if (syncSignature == null) {
                syncSignature = task.getSyncSignature();
            }
            return true;
        }

        void startThreadIfNeeded() {
            if (counter.get() >= maxThreadNum) {
                logger.debug(String.format("syncSignature: %s reached maxThreadNum: %s, current: %d", syncSignature, maxThreadNum, counter.get()));
                return;
            }

            counter.incrementAndGet();
            _threadFacade.submit(new Task<Void>() {
                @Override
                public String getName() {
                    return "sync-chain-thread";
                }

                // start a new thread every time to avoid stack overflow
                @AsyncThread
                private void runQueue() {
                    ChainFuture cf;
                    synchronized (chainTasks) {
                        // remove from pending queue and add to running queue later
                        cf = (ChainFuture) pendingQueue.poll();

                        if (cf == null) {
                            if (counter.decrementAndGet() == 0) {
                                chainTasks.remove(syncSignature);
                            }

                            return;
                        }
                    }

                    synchronized (runningQueue) {
                        processTimeoutTask(cf);
                        cf.startExecutionTimeInMills = zTimer.getCurrentTimeMillis();
                        // add to running queue
                        logger.debug(String.format("Start executing runningQueue: %s, task name: %s", syncSignature, cf.getTask().getName()));
                        runningQueue.offer(cf);
                        Optional.ofNullable(getApiId(cf))
                                .ifPresent(apiId -> apiRunningSignature.computeIfAbsent(apiId,
                                        k -> Collections.synchronizedList(new ArrayList<>())).add(syncSignature));
                    }

                    if (cf.getTask().getDeduplicateString() != null) {
                        removeSubPending(cf.getTask().getDeduplicateString(), false);
                    }

                    // recover task context from backup
                    if (cf.getTask().taskContext != null) {
                        TaskContext.setTaskContext(cf.getTask().taskContext);
                    } else {
                        TaskContext.removeTaskContext();
                    }

                    cf.run(() -> {
                        synchronized (runningQueue) {
                            Optional.ofNullable(getApiId(cf))
                                    .ifPresent(apiId -> apiRunningSignature.computeIfPresent(apiId, (k, sigs) -> {
                                        sigs.remove(syncSignature);
                                        return sigs.isEmpty() ? null : sigs;
                                    }));
                            runningQueue.remove(cf);
                            logger.debug(String.format("Finish executing runningQueue: %s, task name: %s", syncSignature, cf.getTask().getName()));

                            if (cf.getTask().getDeduplicateString() != null) {
                                removeSubPendingZero(cf.getTask().getDeduplicateString());
                            }
                        }

                        runQueue();
                    });
                }


                private String getApiId(DispatchQueueImpl.ChainFuture cf) {
                    Map<String, String> tc = cf.getTask().getThreadContext();
                    if (tc != null) {
                        return tc.get(Constants.THREAD_CONTEXT_API);
                    } else {
                        return null;
                    }
                }

                @Override
                public Void call() {
                    runQueue();
                    return null;
                }
            });
        }
    }

    @ExceptionSafe
    private void processTimeoutTask(ChainFuture cf) {
        long now = System.currentTimeMillis();
        PendingTaskInfo taskInfo = TaskInfoBuilder.buildPendingTaskInfo(cf, now, 0);
        Double timeout = 0.0;
        if (taskInfo.getContext().isEmpty()) {
            return;
        }

        for (String c : taskInfo.getContextList()) {
            Map context = JSONObjectUtil.toObject(c, LinkedHashMap.class);
            timeout = (context.get("timeout") == null) ? 0 : (Double) context.get("timeout");
        }

        if (timeout > 0 && taskInfo.getPendingTime() * 1000 > timeout.longValue()){
            logger.warn(String.format("this task has been pending for %s ms longer than timeout %s ms, cancel it. task info: %s",
                    taskInfo.getPendingTime()*1000, timeout, taskInfo.toString()));
            cf.cancel(true);
        }
    }

    private <T> Future<T> doChainSyncSubmit(final ChainTask task) {
        assert task.getSyncSignature() != null : "How can you submit a chain task without sync signature ???";
        DebugUtils.Assert(task.getSyncLevel() >= 1, "getSyncLevel() must return 1 at least ");

        synchronized (chainTasks) {
            final String signature = task.getSyncSignature();
            ChainTaskQueueWrapper wrapper = chainTasks.get(signature);
            if (wrapper == null) {
                wrapper = new ChainTaskQueueWrapper();
                chainTasks.put(signature, wrapper);
            }

            ChainFuture cf = new ChainFuture(task);
            boolean succeed = wrapper.addTask(cf, task.getMaxPendingTasks());
            if (!succeed) {
                cf.cancel();
                logger.debug(String.format("Pending queue[%s] exceed max size, task name: %s, start execute callback", task.getSyncSignature(), task.getName()));
                task.exceedMaxPendingCallback();
            } else {
                wrapper.startThreadIfNeeded();
            }
            return cf;
        }
    }


    @Override
    public Future<Void> chainSubmit(ChainTask task) {
        // backup task context for each chain task
        if (TaskContext.getTaskContext() != null) {
            task.taskContext = new HashMap<>(TaskContext.getTaskContext());
        }

        return doChainSyncSubmit(task);
    }

    @Override
    public Map<String, SyncTaskStatistic> getSyncTaskStatistics() {
        Map<String, SyncTaskStatistic> ret = new ConcurrentHashMap<>();
        synchronized (syncTasks) {
            for (SyncTaskQueueWrapper wrapper : syncTasks.values()) {
                SyncTaskStatistic statistic = new SyncTaskStatistic(
                        wrapper.syncSignature,
                        wrapper.maxThreadNum,
                        wrapper.counter.intValue(),
                        wrapper.queue.size()
                );
                ret.put(statistic.getSyncSignature(), statistic);

                logger.warn(JSONObjectUtil.toJsonString(statistic));
            }
        }

        return ret;
    }

    @Override
    public Map<String, ChainTaskStatistic> getChainTaskStatistics() {
        Map<String, ChainTaskStatistic> ret =  new ConcurrentHashMap<>();
        synchronized (chainTasks) {
            for (ChainTaskQueueWrapper wrapper : chainTasks.values()) {
                ChainTaskStatistic statistic = new ChainTaskStatistic(
                        wrapper.syncSignature,
                        wrapper.maxThreadNum,
                        wrapper.counter.intValue(),
                        wrapper.pendingQueue.size()
                );
                ret.put(statistic.getSyncSignature(), statistic);
            }
        }
        return ret;
    }

    @Override
    public boolean isChainTaskRunning(String signature) {
        synchronized (chainTasks) {
            return chainTasks.containsKey(signature);
        }
    }
}
