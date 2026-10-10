package org.zstack.kvm.memory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Conservative presentation of API actions. These lists are hints only; all
 * writes still pass the durable task, license, native capability and CAS gates.
 */
final class MemoryAllowedActionRules {
    private MemoryAllowedActionRules() { }

    static Decision evaluate(boolean licensed, String scope,
                             Map<String, MemoryFieldCapability> fields,
                             MemoryStateVO hostState, MemoryStateInventory hostStateView, long now) {
        List<String> allowed = new ArrayList<>();
        List<String> preflight = new ArrayList<>();
        if (!licensed) {
            allowed.add("preview");
            return new Decision(allowed, preflight);
        }

        if ("VM".equals(scope)) {
            allowed.addAll(Arrays.asList("preview", "apply", "clearOverride", "reconcile"));
            return new Decision(allowed, preflight);
        }
        if (!"Host".equals(scope) && !"Cluster".equals(scope) && !"Global".equals(scope)) {
            allowed.add("preview");
            allowed.add("reconcile");
            return new Decision(allowed, preflight);
        }

        if ("Host".equals(scope)) {
            MemoryCapabilityRules.restrict(fields, hostState, now);
            boolean hasWritableField = fields != null && fields.values().stream().anyMatch(field -> field.writable);
            boolean evidenceFresh = hasFreshNativeEvidence(hostState, now);
            boolean capabilityUnknown = hasUnknownCapability(fields, hostState, now);
            allowed.add("preview");
            if (hasWritableField || !evidenceFresh || capabilityUnknown) {
                allowed.add("apply");
                allowed.add("clearOverride");
            }

            // Lifecycle actions are presented only when the exact shared state
            // projection has proved them eligible. Never infer resume/drain
            // eligibility from license or capability alone.
            List<String> stateActions = hostStateView == null
                    ? Collections.emptyList() : hostStateView.getAllowedActions();
            for (String action : Arrays.asList("pause", "drain", "resume", "reconcile")) {
                if (stateActions.contains(action)) { allowed.add(action); }
            }

            boolean backendPreparation = addPreparationPreflight(preflight, MemoryBackendPreparationRules.ACTION,
                    hostState, "writeback", now);
            boolean poolPreparation = addPreparationPreflight(preflight, MemoryZramPoolPreparationRules.ACTION,
                    hostState, "zram", now);
            if (backendPreparation) { allowed.add(MemoryBackendPreparationRules.ACTION); }
            if (poolPreparation) { allowed.add(MemoryZramPoolPreparationRules.ACTION); }
            boolean evidenceUnknown = !evidenceFresh || capabilityUnknown;
            if (evidenceUnknown) {
                preflight.add("apply");
                preflight.add("clearOverride");
            }
        } else if ("Global".equals(scope) || "Cluster".equals(scope)) {
            // Scope-level requests may enter the normal preview/apply workflow,
            // while the subset that depends on target Hosts is marked for preflight.
            allowed.addAll(Arrays.asList("preview", "apply", "pause", "drain", "resume", "reconcile"));
            preflight.addAll(Arrays.asList("apply", "pause", "drain", "resume", "reconcile"));
            if ("Cluster".equals(scope)) {
                allowed.add("clearOverride");
                preflight.add("clearOverride");
            }
        }

        return new Decision(allowed, unique(preflight));
    }

    private static boolean addPreparationPreflight(List<String> actions, String action,
                                                   MemoryStateVO state, String capability, long now) {
        String reason = MemoryPolicyAdmissionRules.capabilityReason(state, capability, now);
        if (reason == null || !isExplicitlyUnsupported(state, capability, now)) {
            actions.add(action);
            return true;
        }
        return false;
    }

    private static boolean hasFreshNativeEvidence(MemoryStateVO state, long now) {
        if (state == null || state.getLastSampleTime() == null || state.getCapabilities() == null) {
            return false;
        }
        long age = now - state.getLastSampleTime();
        return state.getLastSampleTime() <= now + 5000
                && age >= 0 && age < MemoryOptimizationGlobalConfig.displayTtlMillis();
    }

    private static boolean hasUnknownCapability(Map<String, MemoryFieldCapability> fields,
                                                MemoryStateVO state, long now) {
        if (fields == null) { return false; }
        for (Map.Entry<String, MemoryFieldCapability> item : fields.entrySet()) {
            String reason = item.getValue().reasonCode;
            if ("CAPABILITY_UNKNOWN".equals(reason) || "CAPABILITY_STALE".equals(reason)) { return true; }
            if ("CAPABILITY_UNAVAILABLE".equals(reason)) {
                String name = item.getKey();
                String capability = name.startsWith("ksm.zero") ? "ksmZeroPages"
                        : name.startsWith("ksm.") ? "ksm" : name.startsWith("writeback.") ? "writeback" : "zram";
                if (!isExplicitlyUnsupported(state, capability, now)) { return true; }
            }
        }
        return false;
    }

    /** Distinguish an omitted capability from a fresh, explicit unsupported result. */
    private static boolean isExplicitlyUnsupported(MemoryStateVO state, String capability, long now) {
        if (!hasFreshNativeEvidence(state, now)) {
            return false;
        }
        try {
            Map<String, Object> values = new com.google.gson.Gson().fromJson(
                    state.getCapabilities(), new com.google.gson.reflect.TypeToken<Map<String, Object>>() { }.getType());
            if (values == null) {
                return false;
            }
            return Boolean.FALSE.equals(values.get("supported"))
                    || Boolean.TRUE.equals(values.get("supported"))
                    && Boolean.FALSE.equals(values.get(capability));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static List<String> unique(List<String> values) {
        return new ArrayList<>(new java.util.LinkedHashSet<>(values));
    }

    static final class Decision {
        private final List<String> allowedActions;
        private final List<String> preflightRequiredActions;

        Decision(List<String> allowedActions, List<String> preflightRequiredActions) {
            this.allowedActions = Collections.unmodifiableList(new ArrayList<>(new java.util.LinkedHashSet<>(allowedActions)));
            this.preflightRequiredActions = Collections.unmodifiableList(
                    new ArrayList<>(new java.util.LinkedHashSet<>(preflightRequiredActions)));
        }

        List<String> getAllowedActions() { return allowedActions; }
        List<String> getPreflightRequiredActions() { return preflightRequiredActions; }
    }
}
