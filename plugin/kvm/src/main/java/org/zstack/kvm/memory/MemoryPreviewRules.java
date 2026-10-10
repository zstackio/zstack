package org.zstack.kvm.memory;

import java.util.LinkedHashMap;
import java.util.Map;

/** Resolve only missing Host defaults; a preview never persists policy. */
final class MemoryPreviewRules {
    private MemoryPreviewRules() { }

    static boolean needsCapacity(MemoryPolicyInventory inventory) {
        MemoryPolicyConfig policy = MemoryPolicyRules.decode(inventory.getEffectivePolicy());
        return "Host".equals(inventory.getScope()) && policy.zram != null
                && Boolean.TRUE.equals(policy.zram.enabled)
                && (policy.zram.logicalCapacityBytes == null || policy.zram.ramLimitBytes == null);
    }

    static void resolveCapacity(MemoryPolicyInventory inventory, MemoryAgentResponse response) {
        if (response == null || !response.isSuccess() || response.state == null) {
            throw new IllegalArgumentException("CAPACITY_DEFAULT_UNKNOWN");
        }
        Object defaults = response.state.get("policyDefaults");
        Object zram = defaults instanceof Map ? ((Map<?, ?>) defaults).get("zram") : null;
        if (!(zram instanceof Map)) throw new IllegalArgumentException("CAPACITY_DEFAULT_UNKNOWN");
        MemoryPolicyConfig policy = MemoryPolicyRules.decode(inventory.getEffectivePolicy());
        Map<String, Object> values = new LinkedHashMap<>();
        if (policy.zram.logicalCapacityBytes == null) {
            values.put("logicalCapacityBytes", requiredDefault((Map<?, ?>) zram, "logicalCapacityBytes"));
        }
        if (policy.zram.ramLimitBytes == null) {
            values.put("ramLimitBytes", requiredDefault((Map<?, ?>) zram, "ramLimitBytes"));
        }
        Map<String, Object> patch = new LinkedHashMap<>();
        patch.put("zram", values);
        String json = MemoryPolicyRules.canonicalRequest(patch);
        // Validate both before modifying either inventory field.
        String own = MemoryPolicyRules.merge(inventory.getPolicy(), json, "Host");
        String effective = MemoryPolicyRules.merge(inventory.getEffectivePolicy(), json, "Host");
        inventory.setPolicy(own);
        inventory.setEffectivePolicy(effective);
    }

    private static Object requiredDefault(Map<?, ?> defaults, String field) {
        Object value = defaults.get(field);
        if (value == null) throw new IllegalArgumentException("CAPACITY_DEFAULT_UNKNOWN");
        return value;
    }
}
