package org.zstack.core.telemetry;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Centralizes Sentry initialization so Platform is not coupled to Sentry. DSN and tracesSampleRate
 * are resolved here and passed to Sentry.init(). The single init entry is TelemetryFacadeImpl
 * (calls initIfNeeded after building the OpenTelemetry SDK). CloudBus only uses Sentry.isEnabled()
 * to decide whether to report errors and does not call initIfNeeded.
 */
public final class SentryInitHelper {
    private static final CLogger logger = Utils.getLogger(SentryInitHelper.class);
    private static final Object INIT_LOCK = new Object();

    private SentryInitHelper() {
    }

    /**
     * Initializes Sentry from config if not yet initialized; no-op if already initialized or not
     * configured. Thread-safe; concurrent calls result in a single init.
     *
     * @return true if Sentry is available (can create SentrySpanExporter or use Sentry API)
     */
    public static boolean initIfNeeded() {
        if (Sentry.isEnabled()) {
            logger.trace("SentryInitHelper.initIfNeeded: Sentry already enabled, return true");
            return true;
        }
        if (!CloudBusGlobalProperty.SENTRY_ON) {
            logger.trace("SentryInitHelper.initIfNeeded: CloudBus.sentryOn=false, return false");
            return false;
        }

        synchronized (INIT_LOCK) {
            if (Sentry.isEnabled()) {
                logger.trace("SentryInitHelper.initIfNeeded: Sentry already enabled (by another thread), return true");
                return true;
            }
            logger.trace("SentryInitHelper.initIfNeeded: Sentry.isEnabled()=false, CloudBus.sentryOn=true, resolving DSN");
            String dsn = resolveDsn();
            logger.trace("SentryInitHelper.initIfNeeded: resolveDsn() returned " + (dsn == null || dsn.isEmpty() ? "null/empty" : "non-empty (length=" + dsn.length() + ")"));
            if (dsn == null || dsn.isEmpty()) {
                logger.trace("Sentry skipped: no DSN configured (set Telemetry.sentryDsn or sentry.dsn or SENTRY_DSN to enable)");
                return false;
            }

            try {
                double tracesSampleRate = TelemetryGlobalProperty.SENTRY_TRACES_SAMPLE_RATE;
                logger.trace("SentryInitHelper.initIfNeeded: calling Sentry.init() with tracesSampleRate=" + tracesSampleRate);
                String finalDsn = dsn;
                Sentry.init(options -> {
                    options.setDsn(finalDsn);
                    options.setTracesSampleRate(tracesSampleRate);
                    options.setMaxQueueSize(1000);
                    if (TelemetryGlobalProperty.SENTRY_DEBUG) {
                        options.setDebug(true);
                        options.setDiagnosticLevel(SentryLevel.DEBUG);
                        options.setLogger(new io.sentry.ILogger() {
                            @Override
                            public void log(SentryLevel level, String message, Object... args) {
                                String formatted = args.length > 0 ? String.format(message, args) : message;
                                switch (level) {
                                    case ERROR: logger.warn("[Sentry] " + formatted); break;
                                    case WARNING: logger.warn("[Sentry] " + formatted); break;
                                    default: logger.info("[Sentry] " + formatted); break;
                                }
                            }

                            @Override
                            public void log(SentryLevel level, String message, Throwable throwable) {
                                switch (level) {
                                    case ERROR: logger.warn("[Sentry] " + message, throwable); break;
                                    case WARNING: logger.warn("[Sentry] " + message, throwable); break;
                                    default: logger.info("[Sentry] " + message, throwable); break;
                                }
                            }

                            @Override
                            public void log(SentryLevel level, Throwable throwable, String message, Object... args) {
                                String formatted = args.length > 0 ? String.format(message, args) : message;
                                log(level, formatted, throwable);
                            }

                            @Override
                            public boolean isEnabled(SentryLevel level) {
                                return true;
                            }
                        });
                    }
                });
                logger.info("Sentry initialized (tracesSampleRate=" + tracesSampleRate + ")");
                return true;
            } catch (Exception e) {
                logger.warn("Failed to initialize Sentry, continuing without it: " + e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * Checks only that config is present (CloudBus.sentryOn + DSN); does not call Sentry.init().
     * Used by SentryExporterProvider.isAvailable() to avoid triggering Sentry/OpenTelemetry global
     * registration before buildExporters.
     */
    public static boolean isConfigPresent() {
        if (!CloudBusGlobalProperty.SENTRY_ON) {
            return false;
        }
        String dsn = resolveDsn();
        return dsn != null && !dsn.trim().isEmpty();
    }

    /**
     * DSN resolution order: Telemetry.sentryDsn -> sentry.dsn system property -> SENTRY_DSN env.
     */
    private static String resolveDsn() {
        if (TelemetryGlobalProperty.SENTRY_DSN != null && !TelemetryGlobalProperty.SENTRY_DSN.trim().isEmpty()) {
            logger.trace("SentryInitHelper.resolveDsn: using Telemetry.sentryDsn (length=" + TelemetryGlobalProperty.SENTRY_DSN.trim().length() + ")");
            return TelemetryGlobalProperty.SENTRY_DSN.trim();
        }
        String sysDsn = System.getProperty("sentry.dsn");
        if (sysDsn != null && !sysDsn.trim().isEmpty()) {
            logger.trace("SentryInitHelper.resolveDsn: using System property sentry.dsn");
            return sysDsn.trim();
        }
        String envDsn = System.getenv("SENTRY_DSN");
        if (envDsn != null && !envDsn.trim().isEmpty()) {
            logger.trace("SentryInitHelper.resolveDsn: using env SENTRY_DSN");
            return envDsn.trim();
        }
        logger.trace("SentryInitHelper.resolveDsn: no DSN from any source");
        return null;
    }
}
