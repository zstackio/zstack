package org.zstack.core.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import org.zstack.header.Component;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TelemetryMetricsFacadeImpl implements TelemetryMetricsFacade, Component {
    private static final CLogger logger = Utils.getLogger(TelemetryMetricsFacadeImpl.class);
    private static final String INSTRUMENTATION_SCOPE = "org.zstack.metrics";

    private static final String METRIC_PREFIX = "zstack_";
    private static final String POOL_LABEL = "pool";
    private static final String TASK_TYPE_LABEL = "task_type";
    private static final String SIGNATURE_LABEL = "signature";
    private static final String STATUS_LABEL = "status";

    private SdkMeterProvider meterProvider;
    private MetricReader prometheusReader;
    private Meter meter;
    private volatile boolean initialized = false;

    private final Map<String, AtomicLong> threadPoolActiveCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> threadPoolSize = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> threadPoolMaxSize = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> threadPoolQueueSize = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> threadPoolCompletedCount = new ConcurrentHashMap<>();

    private final Map<String, AtomicLong> chainTaskPendingCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> chainTaskRunningCount = new ConcurrentHashMap<>();

    private final Map<String, AtomicLong> syncTaskPendingCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> syncTaskRunningCount = new ConcurrentHashMap<>();

    private LongHistogram taskWaitTimeHistogram;
    private LongHistogram taskExecutionTimeHistogram;
    private LongCounter taskSubmittedCounter;
    private LongCounter taskCompletedCounter;

    @Override
    public boolean start() {
        if (!TelemetryGlobalProperty.ENABLED || !TelemetryGlobalProperty.METRICS_ENABLED) {
            logger.info("Telemetry metrics disabled by configuration");
            return true;
        }

        try {
            initializeMetrics();
            initialized = true;
            logger.info(String.format("Telemetry metrics initialized on port %d",
                    TelemetryGlobalProperty.PROMETHEUS_PORT));
        } catch (Exception e) {
            logger.error("Failed to initialize telemetry metrics", e);
            initialized = false;
        }

        return true;
    }

    @Override
    public boolean stop() {
        shutdown();
        return true;
    }

    private void initializeMetrics() {
        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), TelemetryGlobalProperty.SERVICE_NAME,
                        AttributeKey.stringKey("deployment.environment"), TelemetryGlobalProperty.ENVIRONMENT)));

        prometheusReader = PrometheusHttpServer.builder()
                .setPort(TelemetryGlobalProperty.PROMETHEUS_PORT)
                .build();

        meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(prometheusReader)
                .build();

        meter = meterProvider.get(INSTRUMENTATION_SCOPE);

        registerThreadPoolGauges();
        registerTaskQueueGauges();
        registerHistogramsAndCounters();
    }

    private void registerThreadPoolGauges() {
        meter.gaugeBuilder(METRIC_PREFIX + "threadpool_active")
                .setDescription("Number of active threads in pool")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    threadPoolActiveCount.forEach((pool, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(POOL_LABEL), pool)));
                });

        meter.gaugeBuilder(METRIC_PREFIX + "threadpool_size")
                .setDescription("Current pool size")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    threadPoolSize.forEach((pool, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(POOL_LABEL), pool)));
                });

        meter.gaugeBuilder(METRIC_PREFIX + "threadpool_max_size")
                .setDescription("Maximum pool size")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    threadPoolMaxSize.forEach((pool, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(POOL_LABEL), pool)));
                });

        meter.gaugeBuilder(METRIC_PREFIX + "threadpool_queue_size")
                .setDescription("Number of tasks waiting in queue")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    threadPoolQueueSize.forEach((pool, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(POOL_LABEL), pool)));
                });

        meter.gaugeBuilder(METRIC_PREFIX + "threadpool_completed_total")
                .setDescription("Total completed tasks")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    threadPoolCompletedCount.forEach((pool, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(POOL_LABEL), pool)));
                });
    }

    private void registerTaskQueueGauges() {
        meter.gaugeBuilder(METRIC_PREFIX + "chaintask_pending")
                .setDescription("Pending chain tasks per signature")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    chainTaskPendingCount.forEach((sig, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(sig))));
                });

        meter.gaugeBuilder(METRIC_PREFIX + "chaintask_running")
                .setDescription("Running chain tasks per signature")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    chainTaskRunningCount.forEach((sig, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(sig))));
                });

        meter.gaugeBuilder(METRIC_PREFIX + "synctask_pending")
                .setDescription("Pending sync tasks per signature")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    syncTaskPendingCount.forEach((sig, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(sig))));
                });

        meter.gaugeBuilder(METRIC_PREFIX + "synctask_running")
                .setDescription("Running sync tasks per signature")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    syncTaskRunningCount.forEach((sig, count) -> measurement.record(count.get(),
                            Attributes.of(AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(sig))));
                });
    }

    private void registerHistogramsAndCounters() {
        taskWaitTimeHistogram = meter.histogramBuilder(METRIC_PREFIX + "task_wait_time_ms")
                .setDescription("Time tasks spend waiting in queue (milliseconds)")
                .ofLongs()
                .build();

        taskExecutionTimeHistogram = meter.histogramBuilder(METRIC_PREFIX + "task_execution_time_ms")
                .setDescription("Task execution time (milliseconds)")
                .ofLongs()
                .build();

        taskSubmittedCounter = meter.counterBuilder(METRIC_PREFIX + "task_submitted_total")
                .setDescription("Total number of tasks submitted")
                .build();

        taskCompletedCounter = meter.counterBuilder(METRIC_PREFIX + "task_completed_total")
                .setDescription("Total number of tasks completed")
                .build();
    }

    /**
     * Sanitize signature for metrics labels.
     * For histogram/counter metrics that don't use tracking maps, only truncates length.
     */
    private String sanitizeSignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return "unknown";
        }
        return signature.length() > 100 ? signature.substring(0, 100) : signature;
    }

    /**
     * Sanitize signature for gauge metrics that use tracking maps.
     * Checks if signature is already tracked, and only limits new signatures when map is full.
     * 
     * @param signature The signature to sanitize
     * @param trackingMap The map used to track this signature (chainTaskPendingCount or syncTaskPendingCount)
     * @return Sanitized signature, or "other" if map is full and signature is not already tracked
     */
    private String sanitizeSignature(String signature, Map<String, AtomicLong> trackingMap) {
        if (signature == null || signature.isEmpty()) {
            return "unknown";
        }
        
        // First truncate to get the normalized form that will be stored in map
        String normalized = signature.length() > 100 ? signature.substring(0, 100) : signature;
        
        // If normalized signature is already tracked, always return it (to maintain label consistency)
        if (trackingMap.containsKey(normalized)) {
            return normalized;
        }
        
        // Only limit new signatures when map is full
        if (trackingMap.size() >= TelemetryGlobalProperty.MAX_TRACKED_SIGNATURES) {
            return "other";
        }
        
        return normalized;
    }

    @Override
    public boolean isEnabled() {
        return TelemetryGlobalProperty.ENABLED && TelemetryGlobalProperty.METRICS_ENABLED && initialized;
    }

    @Override
    public void recordThreadPoolMetrics(String poolName, int activeCount, int poolSize, int maximumPoolSize,
            int queueSize, long completedTaskCount) {
        if (!isEnabled()) {
            return;
        }

        threadPoolActiveCount.computeIfAbsent(poolName, k -> new AtomicLong()).set(activeCount);
        threadPoolSize.computeIfAbsent(poolName, k -> new AtomicLong()).set(poolSize);
        threadPoolMaxSize.computeIfAbsent(poolName, k -> new AtomicLong()).set(maximumPoolSize);
        threadPoolQueueSize.computeIfAbsent(poolName, k -> new AtomicLong()).set(queueSize);
        threadPoolCompletedCount.computeIfAbsent(poolName, k -> new AtomicLong()).set(completedTaskCount);
    }

    @Override
    public void recordChainTaskQueueMetrics(String signature, int pendingCount, int runningCount) {
        if (!isEnabled()) {
            return;
        }

        String sig = sanitizeSignature(signature, chainTaskPendingCount);
        chainTaskPendingCount.computeIfAbsent(sig, k -> new AtomicLong()).set(pendingCount);
        chainTaskRunningCount.computeIfAbsent(sig, k -> new AtomicLong()).set(runningCount);
    }

    @Override
    public void recordSyncTaskQueueMetrics(String signature, int pendingCount, int runningCount) {
        if (!isEnabled()) {
            return;
        }

        String sig = sanitizeSignature(signature, syncTaskPendingCount);
        syncTaskPendingCount.computeIfAbsent(sig, k -> new AtomicLong()).set(pendingCount);
        syncTaskRunningCount.computeIfAbsent(sig, k -> new AtomicLong()).set(runningCount);
    }

    @Override
    public void recordTaskWaitTime(String taskType, String signature, long waitTimeMs) {
        if (!isEnabled() || taskWaitTimeHistogram == null) {
            return;
        }

        Attributes attrs = Attributes.of(
                AttributeKey.stringKey(TASK_TYPE_LABEL), taskType,
                AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(signature));
        taskWaitTimeHistogram.record(waitTimeMs, attrs);
    }

    @Override
    public void recordTaskExecutionTime(String taskType, String signature, long executionTimeMs) {
        if (!isEnabled() || taskExecutionTimeHistogram == null) {
            return;
        }

        Attributes attrs = Attributes.of(
                AttributeKey.stringKey(TASK_TYPE_LABEL), taskType,
                AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(signature));
        taskExecutionTimeHistogram.record(executionTimeMs, attrs);
    }

    @Override
    public void incrementTaskSubmitted(String taskType, String signature) {
        if (!isEnabled() || taskSubmittedCounter == null) {
            return;
        }

        Attributes attrs = Attributes.of(
                AttributeKey.stringKey(TASK_TYPE_LABEL), taskType,
                AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(signature));
        taskSubmittedCounter.add(1, attrs);
    }

    @Override
    public void incrementTaskCompleted(String taskType, String signature, boolean success) {
        if (!isEnabled() || taskCompletedCounter == null) {
            return;
        }

        Attributes attrs = Attributes.of(
                AttributeKey.stringKey(TASK_TYPE_LABEL), taskType,
                AttributeKey.stringKey(SIGNATURE_LABEL), sanitizeSignature(signature),
                AttributeKey.stringKey(STATUS_LABEL), success ? "success" : "error");
        taskCompletedCounter.add(1, attrs);
    }

    @Override
    public void shutdown() {
        if (meterProvider != null) {
            logger.info("Shutting down telemetry metrics...");
            CompletableResultCode result = meterProvider.shutdown();
            
            // Wait for shutdown to complete to ensure Prometheus HTTP server is properly closed and port is released
            try {
                CompletableResultCode shutdownResult = result.join(
                        TelemetryGlobalProperty.SHUTDOWN_TIMEOUT_MS, 
                        TimeUnit.MILLISECONDS);
                
                if (!shutdownResult.isSuccess()) {
                    logger.warn("Telemetry metrics shutdown completed with errors: " + shutdownResult);
                } else {
                    logger.info("Telemetry metrics shutdown completed successfully");
                }
            } catch (Exception e) {
                logger.warn(String.format(
                        "Telemetry metrics shutdown did not complete within %d ms, Prometheus HTTP server may not be properly closed",
                        TelemetryGlobalProperty.SHUTDOWN_TIMEOUT_MS), e);
            }
            meterProvider = null;
            initialized = false;
        }
    }
}
