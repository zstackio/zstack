package org.zstack.core.thread;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.jmx.JmxFacade;
import org.zstack.core.telemetry.TelemetryFacade;
import org.zstack.core.telemetry.TelemetryGlobalProperty;
import org.zstack.core.telemetry.TelemetryMetricsFacade;
import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.core.progress.SingleFlightChainInfo;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ThreadFacadeImpl implements ThreadFacade, ThreadFactory, RejectedExecutionHandler, ThreadFacadeMXBean {
    private static final CLogger _logger = CLoggerImpl.getLogger(ThreadFacadeImpl.class);

    private final Map<PeriodicTask, ScheduledFuture<?>> _periodicTasks = new ConcurrentHashMap<PeriodicTask, ScheduledFuture<?>>();
    private final Map<CancelablePeriodicTask, ScheduledFuture<?>> cancelablePeriodicTasks = new ConcurrentHashMap<CancelablePeriodicTask, ScheduledFuture<?>>();
    private static final AtomicInteger seqNum = new AtomicInteger(0);
    private ScheduledThreadPoolExecutorExt _pool;
    private ScheduledThreadPoolExecutorExt _syncpool;  // for sync tasks
    private ConcurrentHashMap<String, ScheduledThreadPoolExecutorExt> pools = new ConcurrentHashMap<>();
    private DispatchQueue dpq;
    private final TimerPool timerPool = new TimerPool(5);

    @Autowired
    private JmxFacade jmxf;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired(required = false)
    private TelemetryFacade telemetryFacade;
    @Autowired(required = false)
    private TelemetryMetricsFacade metricsFacade;

    private TelemetryFacade getTelemetryFacade() {
        return telemetryFacade;
    }

    private boolean isTelemetryEnabled() {
        return TelemetryGlobalProperty.ENABLED && telemetryFacade != null && telemetryFacade.isEnabled();
    }

    private TelemetryMetricsFacade getMetricsFacade() {
        return metricsFacade;
    }

    private boolean isMetricsEnabled() {
        return TelemetryGlobalProperty.ENABLED && TelemetryGlobalProperty.METRICS_ENABLED
                && metricsFacade != null && metricsFacade.isEnabled();
    }
    
    private void collectAndReportMetrics() {
        TelemetryMetricsFacade metrics = getMetricsFacade();
        if (metrics == null || !metrics.isEnabled()) {
            return;
        }
        
        metrics.recordThreadPoolMetrics("main",
                _pool.getActiveCount(),
                _pool.getPoolSize(),
                _pool.getMaximumPoolSize(),
                _pool.getQueue().size(),
                _pool.getCompletedTaskCount());
        
        metrics.recordThreadPoolMetrics("sync",
                _syncpool.getActiveCount(),
                _syncpool.getPoolSize(),
                _syncpool.getMaximumPoolSize(),
                _syncpool.getQueue().size(),
                _syncpool.getCompletedTaskCount());
        
        pools.forEach((name, pool) -> {
            metrics.recordThreadPoolMetrics(name,
                    pool.getActiveCount(),
                    pool.getPoolSize(),
                    pool.getMaximumPoolSize(),
                    pool.getQueue().size(),
                    pool.getCompletedTaskCount());
        });
        
        Map<String, ChainTaskStatistic> chainStats = dpq.getChainTaskStatistics();
        chainStats.forEach((sig, stat) -> {
            metrics.recordChainTaskQueueMetrics(sig, (int) stat.getPendingTaskNum(), stat.getCurrentRunningThreadNum());
        });
        
        Map<String, SyncTaskStatistic> syncStats = dpq.getSyncTaskStatistics();
        syncStats.forEach((sig, stat) -> {
            metrics.recordSyncTaskQueueMetrics(sig, (int) stat.getPendingTaskNum(), stat.getCurrentRunningThreadNum());
        });
    }

    private static class TimerWrapper extends Timer {
        private int cancelledTimerTaskCount = 0;
        private static final int PURGE_CANCELLED_TIMER_TASK_THRESHOLD = 2000;

        void notifyCancel() {
            if (cancelledTimerTaskCount++ >= PURGE_CANCELLED_TIMER_TASK_THRESHOLD) {
                cancelledTimerTaskCount = 0;
                this.purge();
            }
        }
    }

    private static class TimerPool {
        int poolSize;
        List<TimerWrapper> pool;

        // never use a long type counter for self increment. two issues
        // 1) Java will silently overflow a number; even a long will be overflow someday
        // 2) big number causes extremely bad performance for mod operation
        // instead, reset the counter when it exceeds COUNTER_RESET_THRESHOLD to maintain
        // decent performance for mod operation.
        int counter = 0;
        static final int COUNTER_RESET_THRESHOLD = 1000000;

        private TimerPool(int poolSize) {
            this.poolSize = poolSize;
            pool = new ArrayList<TimerWrapper>(poolSize);
            for (int i = 0; i < poolSize; i++) {
                pool.add(new TimerWrapper());
            }
        }

        TimerWrapper getTimer() {
            int index = ++counter % poolSize;
            if (counter > COUNTER_RESET_THRESHOLD) {
                counter = 0;
            }
            return pool.get(index);
        }

        void stop() {
            for (TimerWrapper wrapper : pool) {
                wrapper.cancel();
            }
        }
    }

    @Override
    public Map<String, SyncTaskStatistic> getSyncTaskStatistics() {
        return dpq.getSyncTaskStatistics();
    }

    @Override
    public Map<String, ChainTaskStatistic> getChainTaskStatistics() {
        return dpq.getChainTaskStatistics();
    }

    @Override
    public ThreadPoolStatistic getThreadPoolStatistic() {
        long completedTask = _pool.getCompletedTaskCount();
        long pendingTask = _pool.getTaskCount() - completedTask;
        return new ThreadPoolStatistic(
                _pool.getPoolSize(),
                _pool.getActiveCount(),
                completedTask,
                pendingTask,
                _pool.getCorePoolSize(),
                _pool.getMaximumPoolSize(),
                _pool.getQueue().size()
        );
    }

    public static class Worker<T> implements Callable<T> {
        private final Task<T> _task;

        public Worker(Task<T> task) {
            _task = task;
        }

        @Override
        public T call() throws Exception {
            try {
                return _task.call();
            } catch (Exception e) {
                _logger.warn(_task.getName() + " throws out an unhandled exception, this thread will terminate immediately", e);
                throw e;
            } catch (Throwable t) {
                _logger.warn(_task.getName() + " throws out an unhandled throwable, this thread will terminate immediately", t);
                throw new CloudRuntimeException(_task.getName() + " throws out an unhandled throwable, this thread will terminate immediately", t);
            }
        }

    }
    
    private class TracedWorker<T> implements Callable<T> {
        private final Task<T> task;
        private final Context parentContext;
        
        TracedWorker(Task<T> task) {
            this.task = task;
            this.parentContext = Context.current();
        }
        
        @Override
        public T call() throws Exception {
            if (!isTelemetryEnabled()) {
                return executeTask();
            }
            
            Span span = null;
            Scope scope = null;
            try {
                span = getTelemetryFacade().getTracer()
                        .spanBuilder("Task: " + task.getName())
                        .setSpanKind(SpanKind.INTERNAL)
                        .setParent(parentContext)
                        .setAttribute("task.name", task.getName())
                        .setAttribute("task.class", task.getClass().getName())
                        .startSpan();
                scope = span.makeCurrent();
                
                T result = executeTask();
                span.setStatus(StatusCode.OK);
                return result;
            } catch (Exception e) {
                if (span != null) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR, e.getMessage());
                }
                throw e;
            } catch (Throwable t) {
                if (span != null) {
                    span.recordException(t);
                    span.setStatus(StatusCode.ERROR, t.getMessage());
                }
                throw new CloudRuntimeException(task.getName() + " throws out an unhandled throwable", t);
            } finally {
                if (scope != null) {
                    scope.close();
                }
                if (span != null) {
                    span.end();
                }
            }
        }
        
        private T executeTask() throws Exception {
            try {
                return task.call();
            } catch (Exception e) {
                _logger.warn(task.getName() + " throws out an unhandled exception, this thread will terminate immediately", e);
                throw e;
            } catch (Throwable t) {
                _logger.warn(task.getName() + " throws out an unhandled throwable, this thread will terminate immediately", t);
                throw new CloudRuntimeException(task.getName() + " throws out an unhandled throwable, this thread will terminate immediately", t);
            }
        }
    }

    @Override
    public int getSyncThreadNum(int totalThreadNum) {
        int n = totalThreadNum / 3;
        return Math.min(totalThreadNum, Math.max(n, 150));
    }

    public void init() {
        int totalThreadNum = ThreadGlobalProperty.MAX_THREAD_NUM;
        if (totalThreadNum < 10) {
            _logger.warn(String.format("ThreadFacade.maxThreadNum is configured to %s, which is too small for running zstack. Change it to 10", ThreadGlobalProperty.MAX_THREAD_NUM));
            totalThreadNum = 10;
        }
        _pool = new ScheduledThreadPoolExecutorExt(totalThreadNum, this, this);
        _syncpool = new ScheduledThreadPoolExecutorExt(getSyncThreadNum(totalThreadNum), this, this);
        _logger.debug(String.format("create ThreadFacade with max thread number:%s", totalThreadNum));
        dpq = new DispatchQueueImpl();

        jmxf.registerBean("ThreadFacade", this);
    }

    private void initThreadPool(ThreadPool pool) {
        ScheduledThreadPoolExecutorExt threadExt = new ScheduledThreadPoolExecutorExt(pool.getThreadNum(), this, this);
        pools.put(pool.getSyncSignature(), threadExt);
    }

    public void destroy() {
        _pool.shutdownNow();
        _syncpool.shutdown();
        pools.forEach((queueName, pool) -> {
            _logger.debug(String.format("shutdown thread pool: %s", queueName));
            pool.shutdown();
        });
    }

    @Override
    public <T> Future<T> submit(Task<T> task) {
        _logger.trace(String.format("submit task: %s", task.getName()));
        if (isTelemetryEnabled()) {
            return _pool.submit(new TracedWorker<T>(task));
        }
        return _pool.submit(new Worker<T>(task));
    }

    public <T> Future<T> submitSyncPool(Task<T> task) {
        if (isTelemetryEnabled()) {
            return _syncpool.submit(new TracedWorker<T>(task));
        }
        return _syncpool.submit(new Worker<T>(task));
    }

    @Override
    public <T> Future<T> submitTargetPool(Task<T> task, String signature) {
        ScheduledThreadPoolExecutorExt executorExt = pools.getOrDefault(signature, _syncpool);
        if (isTelemetryEnabled()) {
            return executorExt.submit(new TracedWorker<>(task));
        }
        return executorExt.submit(new Worker<>(task));
    }

    @Override
    public Thread newThread(@Nonnull Runnable arg0) {
        return new Thread(arg0, "zs-thread-" + seqNum.getAndIncrement());
    }

    @Override
    public void rejectedExecution(Runnable arg0, ThreadPoolExecutor arg1) {
        _logger.warn("Task " + arg0.getClass().getSimpleName() + " got rejected by ThreadPool, the pool looks full");
    }

    private Map<PeriodicTask, ScheduledFuture<?>> getPeriodicTasks() {
        return _periodicTasks;
    }

    @Override
    public Future<Void> submitPeriodicTask(final PeriodicTask task, long delay) {
        assert task.getInterval() != 0;
        assert task.getTimeUnit() != null;

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> ret = (ScheduledFuture<Void>) _pool.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (!isTelemetryEnabled()) {
                    runWithoutTracing();
                    return;
                }
                
                Span span = null;
                Scope scope = null;
                try {
                    span = getTelemetryFacade().getTracer()
                            .spanBuilder("PeriodicTask: " + task.getName())
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAttribute("periodic.task.name", task.getName())
                            .setAttribute("periodic.task.class", task.getClass().getName())
                            .setAttribute("periodic.interval", task.getInterval())
                            .setAttribute("periodic.time_unit", task.getTimeUnit().toString())
                            .startSpan();
                    scope = span.makeCurrent();
                    
                    task.run();
                    span.setStatus(StatusCode.OK);
                } catch (Throwable e) {
                    if (span != null) {
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR, e.getMessage());
                    }
                    _logger.warn("An unhandled exception happened during executing periodic task: " + task.getName() + ", cancel it", e);
                    final Map<PeriodicTask, ScheduledFuture<?>> periodicTasks = getPeriodicTasks();
                    final ScheduledFuture<?> ft = periodicTasks.get(task);
                    if (ft != null) {
                        ft.cancel(true);
                        periodicTasks.remove(task);
                    } else {
                        _logger.warn("Not found feature for task " + task.getName()
                                + ", the exception happened too soon, will try to cancel the task next time the exception happens");
                    }
                } finally {
                    if (scope != null) {
                        scope.close();
                    }
                    if (span != null) {
                        span.end();
                    }
                }
            }
            
            private void runWithoutTracing() {
                try {
                    task.run();
                } catch (Throwable e) {
                    _logger.warn("An unhandled exception happened during executing periodic task: " + task.getName() + ", cancel it", e);
                    final Map<PeriodicTask, ScheduledFuture<?>> periodicTasks = getPeriodicTasks();
                    final ScheduledFuture<?> ft = periodicTasks.get(task);
                    if (ft != null) {
                        ft.cancel(true);
                        periodicTasks.remove(task);
                    } else {
                        _logger.warn("Not found feature for task " + task.getName()
                                + ", the exception happened too soon, will try to cancel the task next time the exception happens");
                    }
                }
            }
        }, delay, task.getInterval(), task.getTimeUnit());
        _periodicTasks.put(task, ret);
        return ret;
    }

    @Override
    public Future<Void> submitPeriodicTask(PeriodicTask task) {
        return submitPeriodicTask(task, 0);
    }

    @Override
    public void registerHook(ThreadAroundHook hook) {
        _pool.registerHook(hook);
        _syncpool.registerHook(hook);
    }

    @Override
    public void unregisterHook(ThreadAroundHook hook) {
        _pool.unregisterHook(hook);
        _syncpool.unregisterHook(hook);
    }

    @Override
    public <T> Future<T> syncSubmit(SyncTask<T> task) {
        return dpq.syncSubmit(task);
    }

    @Override
    public Future<Void> chainSubmit(ChainTask task) {
        return dpq.chainSubmit(task);
    }

    @Override
    public <T> Future<T> singleFlightSubmit(SingleFlightTask task) {
        return dpq.singleFlightSubmit(task);
    }

    @Override
    public boolean isChainTaskRunning(String signature) {
        return dpq.isChainTaskRunning(signature);
    }

    @Override
    public ChainInfo getChainTaskInfo(String signature) {
        return dpq.getChainTaskInfo(signature);
    }

    @Override
    public ChainInfo cleanChainTaskInfo(String signature, Integer index, Boolean cleanUp, Boolean isRunningTask) {
        return dpq.cleanChainTaskInfo(signature, index, cleanUp, isRunningTask);
    }

    @Override
    public SingleFlightChainInfo getSingleFlightChainTaskInfo(String signature) {
        return dpq.getSingleFlightChainTaskInfo(signature);
    }

    @Override
    public Set<String> getApiRunningTaskSignature(String apiId) {
        return dpq.getApiRunningTaskSignature(apiId);
    }

    public interface TimeoutTaskReceipt {
        boolean cancel();
    }

    @Override
    public TimeoutTaskReceipt submitTimeoutTask(Runnable task, TimeUnit unit, long delay) {
        final TimerWrapper timer = timerPool.getTimer();

        class TimerTaskWorker extends java.util.TimerTask implements TimeoutTaskReceipt {
            @Override
            @AsyncThread
            public void run() {
                try {
                    task.run();
                } catch (Throwable t) {
                    _logger.warn(String.format("Unhandled exception happened when running %s", task.getClass().getName()), t);
                } finally {
                    this.cancel();
                }
            }

            @Override
            public boolean cancel() {
                boolean ret = super.cancel();
                timer.notifyCancel();
                return ret;
            }
        }

        TimerTaskWorker worker = new TimerTaskWorker();
        timer.schedule(worker, unit.toMillis(delay));
        return worker;
    }

    @Override
    public Runnable submitTimerTask(final TimerTask task, TimeUnit unit, long delay) {
        final TimerWrapper timer = timerPool.getTimer();
        java.util.TimerTask t = new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    if (task.run()) {
                        cancel();
                    }
                } catch (Throwable t) {
                    _logger.warn(String.format("Unhandled exception happened when running %s", task.getClass().getName()), t);
                }
            }
        };

        timer.schedule(t, unit.toMillis(delay));
        return t::cancel;
    }

    @Override
    public boolean start() {
        int totalThreadNum = ThreadGlobalProperty.MAX_THREAD_NUM;

        List<ThreadPool> poolList = new ArrayList<>();
        for (ThreadPoolRegisterExtensionPoint ext : pluginRegistry.getExtensionList(ThreadPoolRegisterExtensionPoint.class)) {
            List<ThreadPool> threadPools = ext.registerThreadPool();
            if (CollectionUtils.isEmpty(threadPools)) {
                throw new CloudRuntimeException("Empty thread pool registration is not supported");
            }

            List<ThreadPool> noSignaturePools = threadPools.stream().filter(pool -> pool.getSyncSignature() == null).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(noSignaturePools)) {
                throw new CloudRuntimeException("Thread pool registration do not allow empty syncSignature");
            }

            List<String> distinctPoolNames = threadPools.stream().map(ThreadPool::getSyncSignature).distinct().collect(Collectors.toList());
            if (distinctPoolNames.size() < threadPools.size()) {
                throw new CloudRuntimeException(String.format("Duplicate thread pool name detected %s", threadPools.stream().map(ThreadPool::getSyncSignature).collect(Collectors.toList())));
            }

            List<ThreadPool> nameDuplicatePool = poolList.stream().filter(pool -> threadPools.stream().anyMatch(newPool -> pool.getSyncSignature().equals(newPool.getSyncSignature()))).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(nameDuplicatePool)) {
                throw new CloudRuntimeException(String.format("Duplicate thread pool name with existing pool %s", nameDuplicatePool.stream().map(ThreadPool::getSyncSignature).collect(Collectors.toList())));
            }
            poolList.addAll(threadPools);
        }

        _logger.debug(String.format("Load separate thread pool: %d", poolList.size()));
        int separatedThreadNum = poolList.stream().mapToInt(ThreadPool::getThreadNum).sum();
        int internalThreadNum = totalThreadNum - separatedThreadNum;
        if (internalThreadNum < 10) {
            _logger.warn(String.format("ThreadFacade.maxThreadNum is configured to %s." +
                            " Remaining thread number for internal pools is %d, which is too" +
                            " small for running zstack. Change it to 10",
                    internalThreadNum,
                    ThreadGlobalProperty.MAX_THREAD_NUM));
            internalThreadNum = 10;
            totalThreadNum = separatedThreadNum + internalThreadNum;
        }

        _logger.debug(String.format("Total thread num: %s, registered thread num %s," +
                        " internal thread num %s", totalThreadNum,
                separatedThreadNum,
                internalThreadNum));
        poolList.forEach(this::initThreadPool);
        
        startMetricsCollection();

        return true;
    }
    
    private void startMetricsCollection() {
        if (!TelemetryGlobalProperty.ENABLED || !TelemetryGlobalProperty.METRICS_ENABLED) {
            return;
        }
        
        int intervalSeconds = TelemetryGlobalProperty.METRICS_COLLECTION_INTERVAL_SECONDS;
        _logger.info(String.format("Starting metrics collection with interval %d seconds", intervalSeconds));
        
        _pool.scheduleAtFixedRate(() -> {
            try {
                collectAndReportMetrics();
            } catch (Throwable e) {
                _logger.trace("Error collecting metrics", e);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean stop() {
        _pool.shutdown();
        _syncpool.shutdown();
        timerPool.stop();
        pools.forEach((queueName, pool) -> {
            _logger.debug(String.format("shutdown thread pool: %s", queueName));
            pool.shutdown();
        });
        return true;
    }

    @Override
    public Future<Void> submitCancelablePeriodicTask(CancelablePeriodicTask task) {
        return submitCancelablePeriodicTask(task, 0);
    }

    @Override
    public Future<Void> submitCancelablePeriodicTask(final CancelablePeriodicTask task, long delay) {
        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> ret = (ScheduledFuture<Void>) _pool.scheduleAtFixedRate(new Runnable() {
            private void cancelTask() {
                ScheduledFuture<?> ft = cancelablePeriodicTasks.get(task);
                if (ft != null) {
                    ft.cancel(true);
                    cancelablePeriodicTasks.remove(task);
                } else {
                    _logger.warn("cannot find feature for task " + task.getName()
                            + ", the exception happened too soon, will try to cancel the task next time the exception happens");
                }
            }

            public void run() {
                if (!isTelemetryEnabled()) {
                    runWithoutTracing();
                    return;
                }
                
                Span span = null;
                Scope scope = null;
                try {
                    span = getTelemetryFacade().getTracer()
                            .spanBuilder("CancelablePeriodicTask: " + task.getName())
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAttribute("periodic.task.name", task.getName())
                            .setAttribute("periodic.task.class", task.getClass().getName())
                            .setAttribute("periodic.interval", task.getInterval())
                            .setAttribute("periodic.time_unit", task.getTimeUnit().toString())
                            .setAttribute("periodic.cancelable", true)
                            .startSpan();
                    scope = span.makeCurrent();
                    
                    boolean cancel = task.run();
                    if (cancel) {
                        span.setAttribute("periodic.cancelled", true);
                        cancelTask();
                    }
                    span.setStatus(StatusCode.OK);
                } catch (Throwable e) {
                    if (span != null) {
                        span.recordException(e);
                        span.setStatus(StatusCode.ERROR, e.getMessage());
                    }
                    _logger.warn("An unhandled exception happened during executing periodic task: " + task.getName() + ", cancel it", e);
                    cancelTask();
                } finally {
                    if (scope != null) {
                        scope.close();
                    }
                    if (span != null) {
                        span.end();
                    }
                }
            }
            
            private void runWithoutTracing() {
                try {
                    boolean cancel = task.run();
                    if (cancel) {
                        cancelTask();
                    }
                } catch (Throwable e) {
                    _logger.warn("An unhandled exception happened during executing periodic task: " + task.getName() + ", cancel it", e);
                    cancelTask();
                }
            }
        }, delay, task.getInterval(), task.getTimeUnit());
        cancelablePeriodicTasks.put(task, ret);
        return ret;
    }

    @Override
    public void printThreadsAndTasks() {
        long completedTask = _pool.getCompletedTaskCount();
        long pendingTask = _pool.getTaskCount() - completedTask;

        long completedSyncTask = _syncpool.getCompletedTaskCount();
        long pendingSyncTask = _syncpool.getTaskCount() - completedSyncTask;

        StringBuilder builder = new StringBuilder();
        builder.append("check thread poolSize and tasks: ");
        builder.append(String.format("poolSize: %s, activeSize: %s, corePoolSize: %s, maximumPoolSize: %s, " +
                "completedTasks: %s, pendingTasks: %s, queueTasks: %s", _pool.getPoolSize(), _pool.getActiveCount(),
                _pool.getCorePoolSize(), _pool.getMaximumPoolSize(), completedTask, pendingTask, _pool.getQueue().size()));
        builder.append("check sync thread poolSize and tasks: ");
        builder.append(String.format("syncPoolSize: %s, activeSize: %s, coreSyncPoolSize: %s, maximumSyncPoolSize: %s, " +
                        "completedSyncTask: %s, pendingSyncTask: %s, queueSyncTasks: %s", _syncpool.getPoolSize(), _syncpool.getActiveCount(),
                _syncpool.getCorePoolSize(), _syncpool.getMaximumPoolSize(), completedSyncTask, pendingSyncTask,
                _syncpool.getQueue().size()));

        _logger.debug(builder.toString());
    }
}
