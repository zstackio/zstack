package org.zstack.core.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.zstack.header.Component;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TelemetryFacadeImpl implements TelemetryFacade, Component {
    private static final CLogger logger = Utils.getLogger(TelemetryFacadeImpl.class);
    private static final String INSTRUMENTATION_NAME = "org.zstack";

    private OpenTelemetry openTelemetry;
    private SdkTracerProvider tracerProvider;
    private Tracer defaultTracer;
    private volatile boolean initialized = false;

    private final Map<String, TelemetryExporterProvider> exporterProviders = new HashMap<>();

    @Override
    public boolean start() {
        logger.info("TelemetryFacade.start() invoked (Telemetry.enabled=" + TelemetryGlobalProperty.ENABLED + ", exporters=" + TelemetryGlobalProperty.EXPORTERS + ")");
        if (!TelemetryGlobalProperty.ENABLED) {
            logger.info("Telemetry is disabled by configuration");
            return true;
        }

        // Idempotency: avoid buildAndRegisterGlobal() being called when global is already registered
        synchronized (this) {
            logger.trace("TelemetryFacade.start() inside synchronized, initialized=" + initialized);
            if (initialized) {
                logger.info("Telemetry already initialized, skipping re-initialization");
                return true;
            }

            try {
                logger.trace("TelemetryFacade: calling registerBuiltInProviders()");
                registerBuiltInProviders();
                logger.trace("TelemetryFacade: calling initializeOpenTelemetry(), providers=" + exporterProviders.keySet());
                initializeOpenTelemetry();
                initialized = true;
                logger.info(String.format("Telemetry initialized successfully: environment=%s, exporters=%s",
                        TelemetryGlobalProperty.ENVIRONMENT, TelemetryGlobalProperty.EXPORTERS));
            } catch (Exception e) {
                logger.error("Failed to initialize telemetry", e);
                initialized = false;
            }
        }

        return true;
    }

    private void registerBuiltInProviders() {
        logger.trace("TelemetryFacade: registering OtlpExporterProvider");
        registerProvider(new OtlpExporterProvider());
        logger.trace("TelemetryFacade: registering SentryExporterProvider");
        registerProvider(new SentryExporterProvider());
    }

    public void registerProvider(TelemetryExporterProvider provider) {
        String name = provider.getName().toLowerCase(Locale.ROOT);
        exporterProviders.put(name, provider);
        boolean available = provider.isAvailable();
        logger.trace(String.format("Registered telemetry exporter provider: %s (available=%s)", provider.getName(), available));
        if ("sentry".equals(name)) {
            logger.info("Sentry exporter provider registered (available=" + available + ")");
        }
    }

    @Override
    public boolean stop() {
        shutdown();
        return true;
    }

    private void initializeOpenTelemetry() {
        logger.trace("TelemetryFacade: building resource (serviceName=" + TelemetryGlobalProperty.SERVICE_NAME + ", environment=" + TelemetryGlobalProperty.ENVIRONMENT + ")");
        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), TelemetryGlobalProperty.SERVICE_NAME,
                        AttributeKey.stringKey("service.version"),
                        TelemetryGlobalProperty.SERVICE_VERSION.isEmpty() ? "unknown"
                                : TelemetryGlobalProperty.SERVICE_VERSION,
                        AttributeKey.stringKey("deployment.environment"), TelemetryGlobalProperty.ENVIRONMENT)));

        ZStackSampler sampler = new ZStackSampler();
        logger.trace("TelemetryFacade: calling buildExporters()");

        List<SpanExporter> exporters = buildExporters();
        logger.trace("TelemetryFacade: buildExporters() returned " + exporters.size() + " exporter(s)");

        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(sampler);

        // Add BatchSpanProcessor for each exporter
        for (SpanExporter exporter : exporters) {
            BatchSpanProcessor batchProcessor = BatchSpanProcessor.builder(exporter)
                    .setMaxExportBatchSize(TelemetryGlobalProperty.MAX_EXPORT_BATCH_SIZE)
                    .setMaxQueueSize(TelemetryGlobalProperty.MAX_QUEUE_SIZE)
                    .setScheduleDelay(TelemetryGlobalProperty.EXPORT_DELAY_MS, TimeUnit.MILLISECONDS)
                    .build();
            tracerProviderBuilder = tracerProviderBuilder.addSpanProcessor(batchProcessor);
        }

        // ErrorKeepingSpanProcessor only needs to be added once, using composite exporter to ensure all exporters receive error spans
        // This avoids the same error span being processed multiple times by different processors
        if (TelemetryGlobalProperty.ALWAYS_SAMPLE_ERRORS && !exporters.isEmpty()) {
            SpanExporter compositeExporter = createCompositeExporter(exporters);
            SpanProcessor errorProcessor = new ErrorKeepingSpanProcessor(compositeExporter);
            tracerProviderBuilder = tracerProviderBuilder.addSpanProcessor(errorProcessor);
        }

        tracerProvider = tracerProviderBuilder.build();
        logger.trace("TelemetryFacade: SdkTracerProvider built");

        // Do not call buildAndRegisterGlobal() to avoid conflict with early registrants like
        // sentry-opentelemetry-bootstrap. All spans are created via TelemetryFacade.getTracer()/
        // startSpan() using this instance's tracerProvider (including Sentry exporter).
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        logger.trace("TelemetryFacade: OpenTelemetry SDK built (not registered globally; use our instance for Sentry)");

        // Single Sentry init entry for CloudBus error reporting and LazySentryExporter
        if (SentryInitHelper.isConfigPresent()) {
            SentryInitHelper.initIfNeeded();
        }

        defaultTracer = openTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    /**
     * Create a composite SpanExporter that exports spans to all provided exporters
     */
    private SpanExporter createCompositeExporter(List<SpanExporter> exporters) {
        if (exporters.size() == 1) {
            return exporters.get(0);
        }

        return new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                List<CompletableResultCode> results = new ArrayList<>();
                for (SpanExporter exporter : exporters) {
                    try {
                        results.add(exporter.export(spans));
                    } catch (Exception e) {
                        logger.warn(String.format("Failed to export spans to exporter %s: %s",
                                exporter.getClass().getSimpleName(), e.getMessage()));
                        results.add(CompletableResultCode.ofFailure());
                    }
                }
                // Wait for all exports to complete, return failure if any fails
                return CompletableResultCode.ofAll(results);
            }

            @Override
            public CompletableResultCode flush() {
                List<CompletableResultCode> results = new ArrayList<>();
                for (SpanExporter exporter : exporters) {
                    try {
                        results.add(exporter.flush());
                    } catch (Exception e) {
                        logger.warn(String.format("Failed to flush exporter %s: %s",
                                exporter.getClass().getSimpleName(), e.getMessage()));
                        results.add(CompletableResultCode.ofFailure());
                    }
                }
                return CompletableResultCode.ofAll(results);
            }

            @Override
            public CompletableResultCode shutdown() {
                List<CompletableResultCode> results = new ArrayList<>();
                for (SpanExporter exporter : exporters) {
                    try {
                        results.add(exporter.shutdown());
                    } catch (Exception e) {
                        logger.warn(String.format("Failed to shutdown exporter %s: %s",
                                exporter.getClass().getSimpleName(), e.getMessage()));
                        results.add(CompletableResultCode.ofFailure());
                    }
                }
                return CompletableResultCode.ofAll(results);
            }
        };
    }

    private List<SpanExporter> buildExporters() {
        List<SpanExporter> exporters = new ArrayList<>();
        String exporterConfig = TelemetryGlobalProperty.EXPORTERS;
        logger.trace("TelemetryFacade.buildExporters: Telemetry.EXPORTERS='" + exporterConfig + "', registeredProviders=" + exporterProviders.keySet());

        if (exporterConfig == null || exporterConfig.trim().isEmpty()) {
            logger.info("No exporters configured, traces will not be exported");
            return exporters;
        }

        List<String> requestedExporters = Arrays.asList(exporterConfig.toLowerCase(Locale.ROOT).split(","));
        logger.trace("TelemetryFacade.buildExporters: requested exporter names=" + requestedExporters);

        for (String exporterName : requestedExporters) {
            String name = exporterName.trim();
            if (name.isEmpty()) {
                continue;
            }
            logger.trace("TelemetryFacade.buildExporters: processing exporter '" + name + "'");

            TelemetryExporterProvider provider = exporterProviders.get(name);
            if (provider == null) {
                logger.warn(String.format("Unknown exporter '%s', available: %s", name, exporterProviders.keySet()));
                continue;
            }
            logger.trace("TelemetryFacade.buildExporters: provider for '" + name + "' found, checking isAvailable()");

            if (!provider.isAvailable()) {
                if ("sentry".equals(name)) {
                    logger.warn("Exporter 'sentry' is not available: ensure CloudBus.sentryOn=true and Telemetry.sentryDsn (or sentry.dsn) is set in zstack.properties, then restart");
                } else {
                    logger.warn(String.format("Exporter '%s' is not available (missing dependencies or configuration)", name));
                }
                continue;
            }

            SpanExporter exporter = provider.createExporter();
            logger.trace("TelemetryFacade.buildExporters: createExporter() for '" + name + "' returned " + (exporter != null ? "non-null" : "null"));
            if (exporter != null) {
                exporters.add(exporter);
                logger.info(String.format("Enabled exporter: %s", name));
            }
        }

        logger.trace("TelemetryFacade.buildExporters: total exporters created=" + exporters.size());
        if (exporters.isEmpty()) {
            logger.warn("No exporters were successfully created, traces will not be exported");
        }

        return exporters;
    }

    @Override
    public boolean isEnabled() {
        return TelemetryGlobalProperty.ENABLED && initialized;
    }

    @Override
    public Tracer getTracer() {
        if (!isEnabled()) {
            return GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
        }
        return defaultTracer;
    }

    @Override
    public Tracer getTracer(String instrumentationName) {
        if (!isEnabled()) {
            return GlobalOpenTelemetry.getTracer(instrumentationName);
        }
        return openTelemetry.getTracer(instrumentationName);
    }

    @Override
    public Span startSpan(String spanName) {
        return startSpan(spanName, Context.current(), null);
    }

    @Override
    public Span startSpan(String spanName, Context parentContext) {
        return startSpan(spanName, parentContext, null);
    }

    @Override
    public Span startSpan(String spanName, Map<String, String> attributes) {
        return startSpan(spanName, Context.current(), attributes);
    }

    @Override
    public Span startSpan(String spanName, Context parentContext, Map<String, String> attributes) {
        if (!isEnabled()) {
            logger.trace("TelemetryFacade.startSpan: skipped (telemetry not enabled), name=" + spanName);
            return Span.getInvalid();
        }

        logger.trace("TelemetryFacade.startSpan: creating span name=" + spanName);
        SpanBuilder spanBuilder = defaultTracer.spanBuilder(spanName);

        if (parentContext != null) {
            spanBuilder.setParent(parentContext);
        }

        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                spanBuilder.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        return spanBuilder.startSpan();
    }

    @Override
    public Context getCurrentContext() {
        return Context.current();
    }

    @Override
    public void recordException(Span span, Throwable throwable) {
        if (span == null || !span.isRecording()) {
            return;
        }
        if (throwable == null) {
            logger.warn("recordException called with null throwable, marking span as error without exception details");
            span.setStatus(StatusCode.ERROR, "Unknown error");
            return;
        }
        span.recordException(throwable);
        String errorMessage = throwable.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = throwable.getClass().getSimpleName();
        }
        span.setStatus(StatusCode.ERROR, errorMessage);
    }

    @Override
    public void markSpanError(Span span, String errorMessage) {
        if (span == null || !span.isRecording()) {
            return;
        }
        span.setStatus(StatusCode.ERROR, errorMessage);
    }

    @Override
    public void shutdown() {
        if (tracerProvider != null) {
            logger.info("Shutting down telemetry...");
            CompletableResultCode shutdownResult = tracerProvider.shutdown();
            
            // Wait for shutdown to complete to avoid span loss
            try {
                CompletableResultCode result = shutdownResult.join(
                        TelemetryGlobalProperty.SHUTDOWN_TIMEOUT_MS, 
                        TimeUnit.MILLISECONDS);
                
                if (!result.isSuccess()) {
                    logger.warn("Telemetry shutdown completed with errors: " + result);
                } else {
                    logger.info("Telemetry shutdown completed successfully");
                }
            } catch (Exception e) {
                logger.warn(String.format(
                        "Telemetry shutdown did not complete within %d ms, some spans may be lost",
                        TelemetryGlobalProperty.SHUTDOWN_TIMEOUT_MS), e);
            }
            
            tracerProvider = null;
            initialized = false;
        }
    }
}
