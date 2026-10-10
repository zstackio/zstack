package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.Map;

/** Replace cached Agent savings, never mutate the persisted control observation. */
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

    static MemoryStateInventory inventory(MemoryStateVO source, Map<String, Object> savings, long now) {
        return inventory(source, savings, now, false, false);
    }

    static MemoryStateInventory inventory(MemoryStateVO source, Map<String, Object> savings, long now,
                                          boolean canResume, boolean canReconcile) {
        MemoryStateInventory out = withSavings(source, savings).toInventory();
        // lastSampleTime remains the control-observation time. The metric
        // timestamp is independent and must not be renewed by a control poll.
        Object time = savings == null ? null : savings.get("sampleTime");
        Object quality = savings == null ? null : savings.get("quality");
        Long sampled = time instanceof Number ? ((Number) time).longValue() : null;
        MemoryStatePresentation.populate(out, now, canResume, canReconcile);
        // This inventory's quality and age describe the metric sample, which
        // may be older than the control observation in MemoryStateVO.
        out.setDataAgeSeconds(sampled == null || sampled > now + 5000 ? null : Math.max(0, (now - sampled) / 1000));
        out.setQuality(sampled == null || sampled > now + 5000 ? "Unknown"
                : now - sampled >= MemoryOptimizationGlobalConfig.displayTtlMillis() ? "Stale"
                : quality instanceof String ? (String) quality : "Unknown");
        return out;
    }
}
