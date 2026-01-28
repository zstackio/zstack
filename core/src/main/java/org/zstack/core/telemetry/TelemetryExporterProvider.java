package org.zstack.core.telemetry;

import io.opentelemetry.sdk.trace.export.SpanExporter;

public interface TelemetryExporterProvider {

    String getName();

    boolean isAvailable();

    SpanExporter createExporter();

    int getOrder();
}
