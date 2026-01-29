package org.zstack.core.telemetry;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Sentry span exporter provider. Does not call Sentry.init() or new SentrySpanExporter() here to
 * avoid registering GlobalOpenTelemetry early. Returns LazySentryExporter; real exporter is
 * created on first export after Sentry init; Sentry's single init is in TelemetryFacadeImpl.
 */
public class SentryExporterProvider implements TelemetryExporterProvider {
    private static final CLogger logger = Utils.getLogger(SentryExporterProvider.class);
    private static final String NAME = "sentry";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        boolean configPresent = SentryInitHelper.isConfigPresent();
        logger.trace("SentryExporterProvider.isAvailable: isConfigPresent()=" + configPresent + " (no Sentry.init here)");
        return configPresent;
    }

    @Override
    public SpanExporter createExporter() {
        logger.trace("SentryExporterProvider.createExporter: returning LazySentryExporter (init deferred)");
        return new LazySentryExporter();
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
