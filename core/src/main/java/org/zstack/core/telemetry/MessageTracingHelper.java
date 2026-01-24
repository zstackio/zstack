package org.zstack.core.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.zstack.header.message.Message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageTracingHelper {

    public static final String TRACEPARENT_KEY = "traceparent";
    public static final String TRACESTATE_KEY = "tracestate";

    private static TextMapPropagator getPropagator() {
        // Use GlobalOpenTelemetry to get the configured propagator
        // This will use W3CTraceContextPropagator if available, or the default propagator
        return GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
    }

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier == null ? Collections.emptySet() : carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };

    private static final TextMapSetter<Map<String, String>> MAP_SETTER = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.put(key, value);
        }
    };

    public static Map<String, String> extractTraceContext() {
        Map<String, String> contextMap = new HashMap<>();
        Context currentContext = Context.current();
        
        // Use TextMapPropagator to inject trace context according to W3C Trace Context specification
        // This uses the configured propagator from GlobalOpenTelemetry (typically W3CTraceContextPropagator)
        getPropagator().inject(currentContext, contextMap, MAP_SETTER);
        
        return contextMap;
    }

    public static Context parseTraceContext(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            return Context.current();
        }

        // Use TextMapPropagator to extract trace context according to W3C Trace Context specification
        // This uses the configured propagator from GlobalOpenTelemetry (typically W3CTraceContextPropagator)
        // This ensures full compliance with the W3C Trace Context specification including proper validation
        return getPropagator().extract(Context.current(), contextMap, MAP_GETTER);
    }

    public static void injectTraceContextToMessage(Message msg, Map<String, String> traceContext) {
        if (msg == null || traceContext == null || traceContext.isEmpty()) {
            return;
        }

        Object headerEntry = msg.getHeaderEntry("__trace__");
        Map<String, Object> headers;
        if (headerEntry instanceof Map) {
            headers = (Map<String, Object>) headerEntry;
        } else {
            headers = new HashMap<>();
        }
        headers.putAll(traceContext);
        msg.putHeaderEntry("__trace__", headers);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> extractTraceContextFromMessage(Message msg) {
        if (msg == null) {
            return new HashMap<>();
        }

        Object headers = msg.getHeaderEntry("__trace__");
        if (headers instanceof Map) {
            Map<String, String> result = new HashMap<>();
            ((Map<?, ?>) headers).forEach((k, v) -> {
                if (k instanceof String && v instanceof String) {
                    result.put((String) k, (String) v);
                }
            });
            return result;
        }

        return new HashMap<>();
    }

    public static TextMapGetter<Map<String, String>> getMapGetter() {
        return MAP_GETTER;
    }

    public static TextMapSetter<Map<String, String>> getMapSetter() {
        return MAP_SETTER;
    }
}
