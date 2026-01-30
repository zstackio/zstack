package org.zstack.core.telemetry;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.sentry.Sentry;
import io.sentry.opentelemetry.SentrySpanExporter;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;

/**
 * Creates SentrySpanExporter lazily on first export/flush/shutdown (after SentryInitHelper.initIfNeeded).
 * Allows TelemetryFacadeImpl to build the OpenTelemetry SDK first, then trigger Sentry, avoiding
 * Sentry/OpenTelemetry registering the global tracer first and causing "already registered".
 */
final class LazySentryExporter implements SpanExporter {
    private static final CLogger logger = Utils.getLogger(LazySentryExporter.class);
    private volatile SpanExporter delegate;

    private SpanExporter getDelegate() {
        if (delegate != null) {
            return delegate;
        }
        synchronized (this) {
            if (delegate != null) {
                return delegate;
            }
            if (!SentryInitHelper.initIfNeeded() || !Sentry.isEnabled()) {
                logger.warn("LazySentryExporter: Sentry not initialized, spans will be dropped");
                return null;
            }
            try {
                delegate = new SentrySpanExporter();
                logger.info("LazySentryExporter: created Sentry span exporter (first use)");
            } catch (Exception e) {
                logger.error("LazySentryExporter: failed to create SentrySpanExporter: " + e.getMessage(), e);
                return null;
            }
        }
        return delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        SpanExporter d = getDelegate();
        if (d == null) {
            return CompletableResultCode.ofSuccess();
        }
        return d.export(spans);
    }

    @Override
    public CompletableResultCode flush() {
        SpanExporter d = getDelegate();
        if (d == null) {
            return CompletableResultCode.ofSuccess();
        }
        return d.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        SpanExporter d = delegate;
        if (d == null) {
            return CompletableResultCode.ofSuccess();
        }
        return d.shutdown();
    }
}
