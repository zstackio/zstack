package org.zstack.core.telemetry;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;

/**
 * A SpanProcessor that ensures error spans are always exported, even if they
 * were
 * not sampled by the initial sampling decision.
 * 
 * This implements "tail-based sampling" for errors: when a span ends with an
 * error
 * status or has recorded exceptions, it will be exported regardless of the
 * original
 * sampling decision.
 * 
 * This is useful for debugging production issues where you want low sampling
 * rates
 * for normal operations but 100% capture of errors.
 */
public class ErrorKeepingSpanProcessor implements SpanProcessor {
    private static final CLogger logger = Utils.getLogger(ErrorKeepingSpanProcessor.class);
    private static final String EXCEPTION_EVENT_NAME = "exception";

    private final SpanExporter exporter;
    private final boolean alwaysSampleErrors;
    private final Object exportLock = new Object();

    public ErrorKeepingSpanProcessor(SpanExporter exporter) {
        this(exporter, TelemetryGlobalProperty.ALWAYS_SAMPLE_ERRORS);
    }

    public ErrorKeepingSpanProcessor(SpanExporter exporter, boolean alwaysSampleErrors) {
        this.exporter = exporter;
        this.alwaysSampleErrors = alwaysSampleErrors;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (!alwaysSampleErrors) {
            return;
        }

        if (span.getSpanContext().isSampled()) {
            return;
        }

        if (hasError(span)) {
            // Synchronize export calls to ensure SpanExporter is not called concurrently
            // as per OpenTelemetry specification: SpanProcessor must serialize calls to the exporter
            synchronized (exportLock) {
                try {
                    CompletableResultCode result = exporter.export(Collections.singletonList(span.toSpanData()));
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("Exported error span: traceId=%s, spanId=%s, name=%s",
                                span.getSpanContext().getTraceId(),
                                span.getSpanContext().getSpanId(),
                                span.getName()));
                    }
                    // Note: export() is asynchronous, but we can check if it's already failed
                    if (result.isDone() && !result.isSuccess()) {
                        logger.warn(String.format("Error span export failed: traceId=%s, spanId=%s",
                                span.getSpanContext().getTraceId(),
                                span.getSpanContext().getSpanId()));
                    }
                } catch (Exception e) {
                    logger.warn(String.format("Failed to export error span: %s", e.getMessage()));
                }
            }
        }
    }

    @Override
    public boolean isEndRequired() {
        return alwaysSampleErrors;
    }

    private boolean hasError(ReadableSpan span) {
        if (span.toSpanData().getStatus().getStatusCode() == StatusCode.ERROR) {
            return true;
        }

        for (EventData event : span.toSpanData().getEvents()) {
            if (EXCEPTION_EVENT_NAME.equals(event.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }
}
