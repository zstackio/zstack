package org.zstack.core.telemetry;

public interface TelemetryMetricsFacade {

    boolean isEnabled();

    void recordThreadPoolMetrics(String poolName, int activeCount, int poolSize, int maximumPoolSize,
            int queueSize, long completedTaskCount);

    void recordChainTaskQueueMetrics(String signature, int pendingCount, int runningCount);

    void recordSyncTaskQueueMetrics(String signature, int pendingCount, int runningCount);

    void recordTaskWaitTime(String taskType, String signature, long waitTimeMs);

    void recordTaskExecutionTime(String taskType, String signature, long executionTimeMs);

    void incrementTaskSubmitted(String taskType, String signature);

    void incrementTaskCompleted(String taskType, String signature, boolean success);

    void shutdown();
}
