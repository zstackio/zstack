package org.zstack.core.telemetry;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class SentryExporterProvider implements TelemetryExporterProvider {
    private static final CLogger logger = Utils.getLogger(SentryExporterProvider.class);
    private static final String NAME = "sentry";
    private static final String SENTRY_EXPORTER_CLASS = "io.sentry.opentelemetry.SentrySpanExporter";

    private volatile Boolean available = null;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        if (available == null) {
            synchronized (this) {
                if (available == null) {
                    available = checkSentryAvailable();
                }
            }
        }
        return available;
    }
    
    private boolean checkSentryAvailable() {
        try {
            Class.forName(SENTRY_EXPORTER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            logger.debug("Sentry OpenTelemetry integration not available on classpath");
            return false;
        } catch (NoClassDefFoundError e) {
            logger.debug("Sentry OpenTelemetry integration class not found at runtime: " + e.getMessage());
            return false;
        } catch (ExceptionInInitializerError e) {
            logger.debug("Sentry OpenTelemetry integration initialization error: " + e.getMessage());
            return false;
        } catch (LinkageError e) {
            logger.debug("Sentry OpenTelemetry integration linkage error: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public SpanExporter createExporter() {
        if (!isAvailable()) {
            logger.warn("Sentry exporter requested but sentry-opentelemetry-core is not on classpath");
            return null;
        }
        
        try {
            Class<?> exporterClass = Class.forName(SENTRY_EXPORTER_CLASS);
            SpanExporter exporter = (SpanExporter) exporterClass.getDeclaredConstructor().newInstance();
            logger.info("Created Sentry span exporter");
            return exporter;
        } catch (Exception e) {
            logger.error(String.format("Failed to create Sentry exporter: %s", e.getMessage()), e);
            return null;
        }
    }
    
    @Override
    public int getOrder() {
        return 100;
    }
}
