package org.zstack.kvm.memory;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.Map;

/** Display metadata is conservative and never substitutes for Agent validation. */
final class MemoryCapabilityRules {
    private MemoryCapabilityRules() { }
    @SuppressWarnings("unchecked")
    static void restrict(Map<String, MemoryFieldCapability> fields, MemoryStateVO state, long now) {
        Map<String, Object> capabilities = null;
        if (state != null && state.getLastSampleTime() != null
                && state.getLastSampleTime() <= now + 5000
                && now - state.getLastSampleTime() < MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            try { capabilities = new Gson().fromJson(state.getCapabilities(), Map.class); }
            catch (RuntimeException ignored) { }
        }
        if (capabilities == null) capabilities = Collections.emptyMap();
        for (Map.Entry<String, MemoryFieldCapability> item : fields.entrySet()) {
            String name = item.getKey();
            String capability = name.startsWith("ksm.zero") ? "ksmZeroPages"
                    : name.startsWith("ksm.") ? "ksm" : name.startsWith("writeback.") ? "writeback" : "zram";
            boolean supported = Boolean.TRUE.equals(capabilities.get("supported"))
                    && Boolean.TRUE.equals(capabilities.get(capability));
            if (!supported) {
                item.getValue().writable = false;
                Object reason = capabilities.get(capability + "ReasonCode");
                if (!(reason instanceof String) || "SUPPORTED".equals(reason)) { reason = capabilities.get("reasonCode"); }
                item.getValue().reasonCode = reason instanceof String && !"SUPPORTED".equals(reason)
                        ? (String) reason : "CAPABILITY_UNAVAILABLE";
            } else if (!item.getValue().writable) {
                item.getValue().reasonCode = "LICENSE_UNAVAILABLE";
            }
        }
    }
}
