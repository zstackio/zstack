package org.zstack.core.telemetry;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Global properties for telemetry/tracing configuration.
 * 
 * Supports environment-aware sampling strategies:
 * - DEV/TEST: Full sampling (100%)
 * - STAGING: 10% sampling + error retention
 * - PROD: Configurable sampling (default 1%) + error retention
 */
@GlobalPropertyDefinition
public class TelemetryGlobalProperty {

    /**
     * Master switch for telemetry. When false, no tracing is performed.
     */
    @GlobalProperty(name = "Telemetry.enabled", defaultValue = "false")
    public static boolean ENABLED;

    /**
     * Environment type: DEV, TEST, STAGING, PROD
     * Determines default sampling strategy.
     */
    @GlobalProperty(name = "Telemetry.environment", defaultValue = "DEV")
    public static String ENVIRONMENT;

    /**
     * Sampling rate for traces (0.0 to 1.0).
     * - 1.0 = 100% (all requests)
     * - 0.1 = 10%
     * - 0.01 = 1%
     * Only used in STAGING/PROD environments; DEV/TEST always sample 100%.
     */
    @GlobalProperty(name = "Telemetry.samplingRate", defaultValue = "0.01")
    public static double SAMPLING_RATE;

    /**
     * When true, always sample traces that contain errors, regardless of sampling
     * rate.
     * Enables tail-based sampling for error retention.
     */
    @GlobalProperty(name = "Telemetry.alwaysSampleErrors", defaultValue = "true")
    public static boolean ALWAYS_SAMPLE_ERRORS;

    /**
     * Comma-separated list of exporters to use.
     * Available: otlp, sentry (if sentry-opentelemetry-core is on classpath)
     * Example: "otlp" or "otlp,sentry"
     * Default: "otlp"
     */
    @GlobalProperty(name = "Telemetry.exporters", defaultValue = "otlp")
    public static String EXPORTERS;

    /**
     * OTLP endpoint for Jaeger/Tempo export.
     * Example: http://localhost:4317 (gRPC) or http://localhost:4318/v1/traces
     * (HTTP)
     */
    @GlobalProperty(name = "Telemetry.otlpEndpoint", defaultValue = "")
    public static String OTLP_ENDPOINT;

    /**
     * Sentry DSN for Sentry export.
     * If empty, uses existing Sentry configuration from CloudBus.sentryDsn.
     */
    @GlobalProperty(name = "Telemetry.sentryDsn", defaultValue = "")
    public static String SENTRY_DSN;

    /**
     * Sentry traces sample rate (0.0 to 1.0).
     * Must be set in Sentry.init() via options.setTracesSampleRate(), otherwise Sentry may drop
     * transactions from OTel SentrySpanExporter and Performance/Traces will be empty.
     * Default 1.0 for full sampling; in production consider 0.1 or lower.
     */
    @GlobalProperty(name = "Telemetry.sentryTracesSampleRate", defaultValue = "1.0")
    public static double SENTRY_TRACES_SAMPLE_RATE;

    /**
     * Service name reported in traces.
     */
    @GlobalProperty(name = "Telemetry.serviceName", defaultValue = "zstack-management-node")
    public static String SERVICE_NAME;

    /**
     * Service version reported in traces.
     */
    @GlobalProperty(name = "Telemetry.serviceVersion", defaultValue = "")
    public static String SERVICE_VERSION;

    /**
     * Maximum number of spans to buffer before export.
     */
    @GlobalProperty(name = "Telemetry.maxExportBatchSize", defaultValue = "512")
    public static int MAX_EXPORT_BATCH_SIZE;

    /**
     * Delay between exports in milliseconds.
     */
    @GlobalProperty(name = "Telemetry.exportDelayMs", defaultValue = "5000")
    public static int EXPORT_DELAY_MS;

    /**
     * Maximum queue size for pending spans.
     */
    @GlobalProperty(name = "Telemetry.maxQueueSize", defaultValue = "2048")
    public static int MAX_QUEUE_SIZE;

    // ==================== Metrics Configuration ====================

    /**
     * Enable/disable metrics collection.
     * When enabled, exposes Prometheus-compatible metrics endpoint.
     */
    @GlobalProperty(name = "Telemetry.metricsEnabled", defaultValue = "true")
    public static boolean METRICS_ENABLED;

    /**
     * HTTP port for Prometheus metrics endpoint.
     * Prometheus can scrape metrics from http://host:port/metrics
     */
    @GlobalProperty(name = "Telemetry.prometheusPort", defaultValue = "9464")
    public static int PROMETHEUS_PORT;

    /**
     * Interval in seconds for collecting thread pool metrics.
     * Lower values provide more granular data but higher overhead.
     */
    @GlobalProperty(name = "Telemetry.metricsCollectionIntervalSeconds", defaultValue = "15")
    public static int METRICS_COLLECTION_INTERVAL_SECONDS;

    /**
     * Maximum number of unique task signatures to track.
     * Prevents unbounded cardinality in metrics labels.
     * Tasks beyond this limit are aggregated under "other".
     */
    @GlobalProperty(name = "Telemetry.maxTrackedSignatures", defaultValue = "500")
    public static int MAX_TRACKED_SIGNATURES;

    /**
     * Timeout in milliseconds for telemetry shutdown.
     * The system will wait for this duration to allow pending spans to be exported
     * before forcefully shutting down.
     */
    @GlobalProperty(name = "Telemetry.shutdownTimeoutMs", defaultValue = "10000")
    public static int SHUTDOWN_TIMEOUT_MS;

    /**
     * Enable Sentry SDK debug logging (outputs to management-server.log via log4j).
     */
    @GlobalProperty(name = "Telemetry.sentryDebug", defaultValue = "false")
    public static boolean SENTRY_DEBUG;

    /**
     * Environment types enumeration
     */
    public enum Environment {
        DEV,
        TEST,
        STAGING,
        PROD;

        public static Environment fromString(String env) {
            if (env == null) {
                return DEV;
            }
            String trimmed = env.trim();
            if (trimmed.isEmpty()) {
                return DEV;
            }
            try {
                return valueOf(trimmed.toUpperCase());
            } catch (IllegalArgumentException e) {
                return DEV;
            }
        }

        public boolean isFullSampling() {
            return this == DEV || this == TEST;
        }
    }

}
