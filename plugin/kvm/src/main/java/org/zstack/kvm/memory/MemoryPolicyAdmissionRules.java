package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

/** Capability admission for changed fields and for already-enabled mechanisms in the full Agent payload. */
final class MemoryPolicyAdmissionRules {
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();
    private MemoryPolicyAdmissionRules() { }

    static Map<String, String> changedFields(String before, String after) {
        Map<String, Object> oldValues = flatten(parse(before), "");
        Map<String, Object> newValues = flatten(parse(after), "");
        Set<String> keys = new TreeSet<>(); keys.addAll(oldValues.keySet()); keys.addAll(newValues.keySet());
        Map<String, String> changed = new LinkedHashMap<>();
        for (String key : keys) {
            Object oldValue = oldValues.get(key), newValue = newValues.get(key);
            if (!Objects.equals(oldValue, newValue)) { changed.put(key, String.valueOf(newValue)); }
        }
        return changed;
    }

    static Map<String, String> blockedFields(Map<String, String> changed, MemoryStateVO state, long now) {
        return blockedFields(changed, state, now, null);
    }

    static Map<String, String> blockedFields(Map<String, String> changed, MemoryStateVO state, long now,
            String effectiveTargetPolicy) {
        Map<String, String> blocked = new LinkedHashMap<>();
        for (Map.Entry<String, String> item : changed.entrySet()) {
            String field = item.getKey();
            String value = item.getValue();
            String capability = capability(field);
            if (capability == null || isSafeDisable(field, value) || mechanismDisabled(changed, field)) { continue; }
            String reason = capabilityReason(state, capability, now);
            if (reason != null) { blocked.put(field, reason); }
        }
        if (effectiveTargetPolicy != null) {
            MemoryPolicyConfig effective = MemoryPolicyRules.decode(effectiveTargetPolicy);
            if (effective.ksm != null && Boolean.TRUE.equals(effective.ksm.enabled)) {
                addActiveBlock(blocked, "ksm.enabled", state, "ksm", now);
            }
            if (effective.ksm != null && Boolean.TRUE.equals(effective.ksm.zeroPagesEnabled)) {
                addActiveBlock(blocked, "ksm.zeroPagesEnabled", state, "ksmZeroPages", now);
            }
            if (effective.zram != null && Boolean.TRUE.equals(effective.zram.enabled)) {
                addActiveBlock(blocked, "zram.enabled", state, "zram", now);
            }
            if (effective.writeback != null && Boolean.TRUE.equals(effective.writeback.enabled)) {
                addActiveBlock(blocked, "writeback.enabled", state, "writeback", now);
            }
        }
        return blocked;
    }

    private static void addActiveBlock(Map<String, String> blocked, String field, MemoryStateVO state,
            String capability, long now) {
        String reason = capabilityReason(state, capability, now);
        if (reason != null) { blocked.putIfAbsent(field, reason); }
    }

    private static boolean isSafeDisable(String field, String value) {
        return ("ksm.enabled".equals(field) || "ksm.zeroPagesEnabled".equals(field)
                || "zram.enabled".equals(field) || "writeback.enabled".equals(field))
                && "false".equals(value);
    }

    private static boolean mechanismDisabled(Map<String, String> changed, String field) {
        String mechanism = field.startsWith("ksm.") ? "ksm" : field.startsWith("zram.") ? "zram"
                : field.startsWith("writeback.") ? "writeback" : null;
        return mechanism != null && "false".equals(changed.get(mechanism + ".enabled"));
    }

    private static String capability(String field) {
        if (field.startsWith("ksm.zeroPagesEnabled")) { return "ksmZeroPages"; }
        if (field.startsWith("ksm.")) { return "ksm"; }
        if (field.startsWith("writeback.")) { return "writeback"; }
        if (field.startsWith("zram.")) { return "zram"; }
        return null;
    }

    /** null means supported; an explicit false enable remains admissible even with unknown capabilities. */
    static String capabilityReason(MemoryStateVO state, String capability, long now) {
        if (state == null || state.getLastSampleTime() == null || state.getCapabilities() == null) {
            return "CAPABILITY_UNKNOWN";
        }
        long age = now - state.getLastSampleTime();
        if (state.getLastSampleTime() > now + 5000 || age >= MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            return "CAPABILITY_STALE";
        }
        Map<String, Object> capabilities;
        try { capabilities = new Gson().fromJson(state.getCapabilities(), MAP_TYPE); }
        catch (RuntimeException e) { return "CAPABILITY_UNKNOWN"; }
        if (capabilities == null) { return "CAPABILITY_UNKNOWN"; }
        if (!Boolean.TRUE.equals(capabilities.get("supported"))) {
            Object overall = capabilities.get("reasonCode");
            return overall instanceof String && !((String) overall).isEmpty() && !"SUPPORTED".equals(overall)
                    ? (String) overall : "CAPABILITY_UNAVAILABLE";
        }
        if (Boolean.TRUE.equals(capabilities.get(capability))) {
            return null;
        }
        Object specific = capabilities.get(capability + "ReasonCode");
        if (specific instanceof String && !((String) specific).isEmpty() && !"SUPPORTED".equals(specific)) {
            return (String) specific;
        }
        Object general = capabilities.get("reasonCode");
        if (general instanceof String
                && !((String) general).isEmpty() && !"SUPPORTED".equals(general)) { return (String) general; }
        return "CAPABILITY_UNAVAILABLE";
    }

    private static Map<String, Object> parse(String json) {
        if (json == null || json.trim().isEmpty()) { return Collections.emptyMap(); }
        Map<String, Object> result = new Gson().fromJson(json, MAP_TYPE);
        return result == null ? Collections.emptyMap() : result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flatten(Map<String, Object> source, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> item : source.entrySet()) {
            String key = prefix.isEmpty() ? item.getKey() : prefix + "." + item.getKey();
            if (item.getValue() instanceof Map) { result.putAll(flatten((Map<String, Object>) item.getValue(), key)); }
            else { result.put(key, item.getValue()); }
        }
        return result;
    }
}
