package org.zstack.kvm.memory;

import org.zstack.utils.gson.JSONObjectUtil;
import java.util.*;
import java.math.BigDecimal;

/** Display metadata is conservative and never substitutes for Agent validation. */
final class MemoryCapabilityRules {
    private MemoryCapabilityRules() { }
    @SuppressWarnings("unchecked")
    static void restrict(Map<String, MemoryFieldCapability> fields, MemoryStateVO state, long now) {
        Map<String, Object> capabilities = null;
        boolean freshFullSample = state != null && state.getLastSampleTime() != null
                && state.getLastSampleTime() <= now + 5000
                && now - state.getLastSampleTime() >= 0
                && now - state.getLastSampleTime() < MemoryOptimizationGlobalConfig.displayTtlMillis();
        if (state != null && state.getCapabilities() != null) {
            try { capabilities = JSONObjectUtil.toObject(state.getCapabilities(), Map.class); }
            catch (RuntimeException ignored) { }
        }
        if (capabilities == null) capabilities = Collections.emptyMap();
        boolean partialKsm = partialObservation(capabilities) != null;
        for (Map.Entry<String, MemoryFieldCapability> item : fields.entrySet()) {
            String name = item.getKey();
            String capability = name.startsWith("ksm.zero") ? "ksmZeroPages"
                    : name.startsWith("ksm.") ? "ksm" : name.startsWith("writeback.") ? "writeback" : "zram";
            String partialReason = partialKsm ? partialCapabilityReason(state, capabilities, capability, now) : null;
            boolean supported = partialKsm ? partialReason == null
                    : freshFullSample && Boolean.TRUE.equals(capabilities.get("supported"))
                    && Boolean.TRUE.equals(capabilities.get(capability));
            if (!supported) {
                // Writeback settings are durable desired values. Backend
                // ownership/readiness gates execution, not persistence;
                // retain writability while exposing the unavailable reason.
                if (!"writeback".equals(capability)) {
                    item.getValue().writable = false;
                }
                Object reason = partialKsm ? partialReason : capabilities.get(capability + "ReasonCode");
                if (!(reason instanceof String) || "SUPPORTED".equals(reason)) { reason = capabilities.get("reasonCode"); }
                item.getValue().reasonCode = reason instanceof String && !"SUPPORTED".equals(reason)
                        ? (String) reason : "CAPABILITY_UNAVAILABLE";
            } else if (!item.getValue().writable) {
                item.getValue().reasonCode = "LICENSE_UNAVAILABLE";
            }
        }
    }

    static boolean hasPartialKsmMarker(MemoryStateVO state) {
        if (state == null || state.getCapabilities() == null) { return false; }
        try {
            Map<?, ?> capabilities = JSONObjectUtil.toObject(state.getCapabilities(), Map.class);
            return capabilities != null && partialObservation(capabilities) != null;
        } catch (RuntimeException ignored) { return false; }
    }

    static String partialKsmCapabilityReason(MemoryStateVO state, String capability, long now) {
        if (state == null || state.getCapabilities() == null) { return "CAPABILITY_UNKNOWN"; }
        try {
            Map<String, Object> capabilities = JSONObjectUtil.toObject(state.getCapabilities(), Map.class);
            return partialCapabilityReason(state, capabilities, capability, now);
        } catch (RuntimeException ignored) { return "CAPABILITY_UNKNOWN"; }
    }

    static String coldKsmAdmissionReason(MemoryStateVO state, long now) {
        if (state == null || !"Unknown".equals(state.getStatus())
                || state.getActiveTaskUuid() != null || state.getControlOperationUuid() != null) {
            return "HOST_OPERATION_UNRESOLVED";
        }
        if (coldKsmObservation(state, now) != null) { return null; }
        String reason = partialKsmCapabilityReason(state, "ksm", now);
        return reason == null ? "LOCAL_CONTROL_FENCE_UNKNOWN" : reason;
    }

    static Map<String, Object> coldKsmObservation(MemoryStateVO state, long now) {
        if (state == null || partialKsmCapabilityReason(state, "ksm", now) != null) { return null; }
        try {
            Map<String, Object> capabilities = JSONObjectUtil.toObject(state.getCapabilities(), Map.class);
            Map<?, ?> partial = partialObservation(capabilities);
            if (partial == null) { return null; }
            if (!Boolean.TRUE.equals(partial.get("coldKsmAdmissionReady"))
                    || !"LOCAL_CONTROL_FENCE_CLEAR".equals(partial.get("coldKsmAdmissionReason"))) {
                return null;
            }
            Map<?, ?> nativeState = state.getState() == null ? null
                    : map(JSONObjectUtil.toObject(state.getState(), Map.class).get("nativeKsmObservation"));
            if (nativeState == null || !Objects.equals(nativeState.get("bootId"), partial.get("bootId"))
                    || !Objects.equals(nativeState.get("sampleTime"), partial.get("sampleTime"))) {
                return null;
            }
            Object bootId = partial.get("bootId");
            Map<?, ?> ksm = map(partial.get("ksm"));
            if (!(bootId instanceof String) || ((String) bootId).trim().isEmpty() || ksm == null
                    || !(ksm.get("enabled") instanceof Boolean)
                    || exactLong(ksm.get("pagesToScan")) == null || exactLong(ksm.get("sleepMillis")) == null) {
                return null;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("bootId", partial.get("bootId"));
            result.put("sampleTime", exactLong(partial.get("sampleTime")));
            return result;
        } catch (RuntimeException malformed) { return null; }
    }

    private static String partialCapabilityReason(MemoryStateVO state, Map<String, Object> capabilities,
            String capability, long now) {
        Map<?, ?> partial = partialObservation(capabilities);
        if (partial == null) {
            return "CAPABILITY_UNKNOWN";
        }
        if (!("ksm".equals(capability) || "ksmZeroPages".equals(capability))) {
            return "CAPABILITY_UNAVAILABLE";
        }
        Long observedAt = exactLong(partial.get("sampleTime"));
        if (observedAt == null) { return "CAPABILITY_UNKNOWN"; }
        if (observedAt > now + 5000 || now - observedAt < 0
                || now - observedAt >= MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            return "CAPABILITY_STALE";
        }
        Map<?, ?> partialCapabilities = map(partial.get("capabilities"));
        if (partialCapabilities == null || !Boolean.TRUE.equals(partialCapabilities.get("supported"))) {
            return "CAPABILITY_UNAVAILABLE";
        }
        if (Boolean.TRUE.equals(partialCapabilities.get(capability))) { return null; }
        Object reason = partialCapabilities.get(capability + "ReasonCode");
        return reason instanceof String && !((String) reason).isEmpty()
                ? (String) reason : "CAPABILITY_UNAVAILABLE";
    }

    private static Map<?, ?> partialObservation(Map<?, ?> capabilities) {
        Map<?, ?> partial = capabilities == null ? null : map(capabilities.get("nativeKsmPartial"));
        return partial != null && "KSM_NATIVE_PARTIAL".equals(partial.get("observationKind")) ? partial : null;
    }

    private static Map<?, ?> map(Object value) { return value instanceof Map ? (Map<?, ?>) value : null; }

    private static Long exactLong(Object value) {
        if (!(value instanceof Number) || value instanceof Boolean) { return null; }
        try { return new BigDecimal(value.toString()).longValueExact(); }
        catch (RuntimeException invalid) { return null; }
    }
}
