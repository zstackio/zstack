package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Replace cached Agent savings, never mutate the persisted observation. */
final class MemoryMetricsPresentation {
    private MemoryMetricsPresentation() { }

    static MemoryStateVO withSavings(MemoryStateVO source, Map<String, Object> savings) {
        Gson gson = new Gson();
        MemoryStateVO copy = gson.fromJson(gson.toJson(source), MemoryStateVO.class);
        JsonObject state;
        try {
            state = new JsonParser().parse(source.getState()).getAsJsonObject();
        } catch (RuntimeException invalid) {
            state = new JsonObject();
        }
        state.add("savings", gson.toJsonTree(savings == null ? Collections.emptyMap() : savings));
        copy.setState(gson.toJson(state));
        return copy;
    }

    /**
     * The savings sample with presentation metadata. Clients use the server's
     * TTL and elapsed time since this response, not their own clocks.
     */
    static Map<String, Object> metrics(Map<String, Object> savings, long now) {
        Map<String, Object> metrics = savings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(savings);
        if (savings != null) {
            metrics.put("displayTtlMillis", MemoryOptimizationGlobalConfig.displayTtlMillis());
            metrics.put("presentationTime", now);
        }
        return metrics;
    }

    /** The quality of the metric sample, which may be older than the Host observation. */
    static String quality(Map<String, Object> savings, long now) {
        Object time = savings == null ? null : savings.get("sampleTime");
        Object quality = savings == null ? null : savings.get("quality");
        Long sampled = time instanceof Number ? ((Number) time).longValue() : null;
        return sampled == null || sampled > now + 5000 ? "Unknown"
                : now - sampled >= MemoryOptimizationGlobalConfig.displayTtlMillis() ? "Stale"
                : quality instanceof String ? (String) quality : "Unknown";
    }
}
