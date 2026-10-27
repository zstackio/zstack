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
        return inventory(source, savings, now, false);
    }

    static MemoryStateInventory inventory(MemoryStateVO source, Map<String, Object> savings, long now,
                                          boolean canResume) {
        return inventory(source, savings, now, canResume, null);
    }

    static MemoryStateInventory inventory(MemoryStateVO source, Map<String, Object> savings, long now,
                                          boolean canResume, Map<String, String> uncertainRecovery) {
        MemoryStateInventory out = withSavings(source, savings).toInventory();
        // lastSampleTime remains the control-observation time. The metric
        // timestamp is independent and must not be renewed by a control poll.
        Object time = savings == null ? null : savings.get("sampleTime");
        Object quality = savings == null ? null : savings.get("quality");
        Long sampled = time instanceof Number ? ((Number) time).longValue() : null;
        MemoryStatePresentation.populate(out, now, canResume);
        if (uncertainRecovery != null) {
            java.util.List<String> actions = new java.util.ArrayList<>(out.getAllowedActions());
            actions.add(MemoryUncertainRecoveryRules.ACTION);
            out.setAllowedActions(actions); out.setUncertainRecovery(uncertainRecovery);
        }
        // This inventory's quality and age describe the metric sample, which
        // may be older than the control observation in MemoryStateVO.
        out.setDataAgeSeconds(sampled == null || sampled > now + 5000 ? null : Math.max(0, (now - sampled) / 1000));
        out.setQuality(sampled == null || sampled > now + 5000 ? "Unknown"
                : now - sampled >= MemoryOptimizationGlobalConfig.displayTtlMillis() ? "Stale"
                : quality instanceof String ? (String) quality : "Unknown");
        // Project the public result only after native eligibility and actions
        // have been evaluated from the original control observation.
        String status = source.getStatus();
        switch (status == null ? "Unknown" : status) {
            case "Queued": case "Applying": case "Draining": case "Unknown":
                break;
            case "Succeeded": case "Failed": case "Blocked":
                if (source.getActiveTaskUuid() != null) {
                    if (!"Blocked".equals(status)) status = "Unknown";
                } else {
                    String plan = source.getPolicyPlanStatus();
                    if ("Blocked".equals(plan) || "NeedsReview".equals(plan)) status = "Blocked";
                    else if ("Failed".equals(plan) || "Unknown".equals(plan)) status = plan;
                    else if ("Queued".equals(plan) || "Applying".equals(plan) || "Draining".equals(plan)) {
                        status = "Unknown";
                    }
                }
                break;
            default:
                status = "Unknown";
        }
        out.setStatus(status == null ? "Unknown" : status);
        return out;
    }
}
