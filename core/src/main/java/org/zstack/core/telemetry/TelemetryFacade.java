package org.zstack.core.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import java.util.Map;

public interface TelemetryFacade {
    
    boolean isEnabled();
    
    Tracer getTracer();
    
    Tracer getTracer(String instrumentationName);
    
    Span startSpan(String spanName);
    
    Span startSpan(String spanName, Context parentContext);
    
    Span startSpan(String spanName, Map<String, String> attributes);
    
    Span startSpan(String spanName, Context parentContext, Map<String, String> attributes);
    
    Context getCurrentContext();
    
    void recordException(Span span, Throwable throwable);
    
    void markSpanError(Span span, String errorMessage);
    
    void shutdown();
}
