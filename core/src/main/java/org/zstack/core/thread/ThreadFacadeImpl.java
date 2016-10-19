package org.zstack.core.thread;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.jmx.JmxFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFacadeImpl implements ThreadFacade, ThreadFactory, RejectedExecutionHandler, ThreadFacadeMXBean {
    private static final CLogger _logger = CLoggerImpl.getLogger(ThreadFacadeImpl.class);

    private int totalThreadNum;

    private Map<PeriodicTask, ScheduledFuture<?>> _periodicTasks = new ConcurrentHashMap<PeriodicTask, ScheduledFuture<?>>();
    private Map<CancelablePeriodicTask, ScheduledFuture<?>> cancelablePeriodicTasks = new ConcurrentHashMap<CancelablePeriodicTask, ScheduledFuture<?>>();
    private static AtomicInteger seqNum = new AtomicInteger(0);
    private ScheduledThreadPoolExecutorExt _pool;
    private DispatchQueue dpq;
    private TimerPool timerPool = new TimerPool(5);

    @Autowired
    private JmxFacade jmxf;

    private class TimerWrapper extends Timer {
        private int cancelledTimerTaskCount = 0;
        private static final int PURGE_CANCELLED_TIMER_TASK_THRESHOLD = 2000;

        void notifyCancel() {
            if (cancelledTimerTaskCount++ >= PURGE_CANCELLED_TIMER_TASK_THRESHOLD) {
                cancelledTimerTaskCount = 0;
                this.purge();
            }
        }
    }

    private class TimerPool {
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

    public void init() {
        totalThreadNum = ThreadGlobalProperty.MAX_THREAD_NUM;
        if (totalThreadNum < 10) {
            _logger.warn(String.format("ThreadFacade.maxThreadNum is configured to %s, which is too small for running zstack. Change it to 10", ThreadGlobalProperty.MAX_THREAD_NUM));
            totalThreadNum = 10;
        }
        _pool = new ScheduledThreadPoolExecutorExt(totalThreadNum, this, this);
        _logger.debug(String.format("create ThreadFacade with max thread number:%s", totalThreadNum));
        dpq = new DispatchQueueImpl();

        jmxf.registerBean("ThreadFacade", this);
    }

    public void destroy() {
        _pool.shutdownNow();
    }

    @Override
    public <T> Future<T> submit(Task<T> task) {
        return _pool.submit(new Worker<T>(task));
    }

    @Override
    public Thread newThread(Runnable arg0) {
        return new Thread(arg0, "zs-thread-" + String.valueOf(seqNum.getAndIncrement()));
    }


    @Override
    public void rejectedExecution(Runnable arg0, ThreadPoolExecutor arg1) {
        StringBuilder warn = new StringBuilder("Task " + arg0.getClass().getSimpleName() + " got rejected by ThreadPool, the pool looks full");
        _logger.warn(warn.toString());
    }

    private Map<PeriodicTask, ScheduledFuture<?>> getPeriodicTasks() {
        return _periodicTasks;
    }

    @Override
    public Future<Void> submitPeriodicTask(final PeriodicTask task, long delay) {
        assert task.getInterval() != 0;
        assert task.getTimeUnit() != null;

        ScheduledFuture<Void> ret = (ScheduledFuture<Void>) _pool.scheduleAtFixedRate(new Runnable() {
            public void run() {
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
    }

    @Override
    public void unregisterHook(ThreadAroundHook hook) {
        _pool.unregisterHook(hook);
    }

    public int getTotalThreadNum() {
        return totalThreadNum;
    }

    public void setTotalThreadNum(int totalThreadNum) {
        this.totalThreadNum = totalThreadNum;
    }

    @Override
    public <T> Future<T> syncSubmit(SyncTask<T> task) {
        return dpq.syncSubmit(task);
    }

    @Override
    public Future<Void> chainSubmit(ChainTask task) {
        return dpq.chainSubmit(task);
    }

    public static interface TimeoutTaskReceipt {
        boolean cancel();
    }

    @Override
    public TimeoutTaskReceipt submitTimeoutTask(final Runnable task, TimeUnit unit, long delay) {
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
    public void submitTimerTask(final TimerTask task, TimeUnit unit, long delay) {
        final TimerWrapper timer = timerPool.getTimer();
        timer.schedule(new java.util.TimerTask() {
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
        }, unit.toMillis(delay));
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        _pool.shutdown();
        timerPool.stop();
        return true;
    }

    @Override
    public Future<Void> submitCancelablePeriodicTask(CancelablePeriodicTask task) {
        return submitCancelablePeriodicTask(task, 0);
    }

    @Override
    public Future<Void> submitCancelablePeriodicTask(final CancelablePeriodicTask task, long delay) {
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
}
