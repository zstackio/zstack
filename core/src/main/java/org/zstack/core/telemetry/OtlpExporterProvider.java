package org.zstack.core.telemetry;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class OtlpExporterProvider implements TelemetryExporterProvider {
    private static final CLogger logger = Utils.getLogger(OtlpExporterProvider.class);
    private static final String NAME = "otlp";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        String endpoint = TelemetryGlobalProperty.OTLP_ENDPOINT;
        return endpoint != null && !endpoint.trim().isEmpty();
    }

    @Override
    public SpanExporter createExporter() {
        String endpoint = TelemetryGlobalProperty.OTLP_ENDPOINT;
        if (endpoint == null || endpoint.trim().isEmpty()) {
            logger.warn("OTLP endpoint not configured, cannot create exporter");
            return null;
        }

        String sanitizedEndpoint = sanitizeEndpointForLogging(endpoint);
        logger.info(String.format("Creating OTLP exporter with endpoint: %s", sanitizedEndpoint));
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sanitize endpoint URL for logging to avoid exposing sensitive information.
     * Only extracts and returns scheme://host:port, hiding path, query parameters, and user info.
     *
     * @param endpoint the full endpoint URL
     * @return sanitized endpoint string safe for logging (scheme://host:port)
     */
    private String sanitizeEndpointForLogging(String endpoint) {
        try {
            UriComponents components = UriComponentsBuilder.fromUriString(endpoint).build();
            StringBuilder sanitized = new StringBuilder();
            
            if (components.getScheme() != null) {
                sanitized.append(components.getScheme()).append("://");
            }
            
            if (components.getHost() != null) {
                sanitized.append(components.getHost());
            }
            
            if (components.getPort() != -1) {
                sanitized.append(":").append(components.getPort());
            }
            
            return sanitized.length() > 0 ? sanitized.toString() : "***";
        } catch (Exception e) {
            // If URL parsing fails, return a masked value
            logger.debug(String.format("Failed to parse endpoint URL for sanitization: %s", e.getMessage()));
            return "***";
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
