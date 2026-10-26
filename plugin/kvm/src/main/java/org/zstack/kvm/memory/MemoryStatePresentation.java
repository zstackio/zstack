package org.zstack.kvm.memory;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.Map;
import java.math.BigDecimal;

/** Read-only, nullable presentation of a cached Host sample. */
final class MemoryStatePresentation {
    private MemoryStatePresentation() { }
    @SuppressWarnings("unchecked")
    static void populate(MemoryStateInventory out, long now) {
        populate(out, now, false);
    }

    @SuppressWarnings("unchecked")
    static void populate(MemoryStateInventory out, long now, boolean canResume) {
        out.setDrift(out.getAppliedRevision() == null ? null
                : out.getDesiredRevision() != out.getAppliedRevision());
        Long sample = out.getLastSampleTime();
        out.setDataAgeSeconds(sample == null || sample > now + 5000 ? null : Math.max(0, (now - sample) / 1000));
        Map<String, Object> state;
        try { state = new Gson().fromJson(out.getState(), Map.class); }
        catch (RuntimeException ignored) { state = null; }
        if (state == null) state = Collections.emptyMap();
        out.setBootId(state.get("bootId") instanceof String ? (String) state.get("bootId") : null);
        Object lifecycle = state.get("lifecycle");
        Object active = lifecycle instanceof Map ? ((Map<?, ?>) lifecycle).get("activeState") : null;
        boolean contradicted = newerPartialContradictsState(out, state, now);
        boolean controlledPool = !contradicted && controlledGoPool(out, state, now);
        if (contradicted) {
            out.setFeatureState("Unknown"); out.setFeatureStateSource(null);
            if (!java.util.Objects.equals(state.get("bootId"), field(state.get("nativeKsmObservation"), "bootId"))) {
                out.setBootId(null);
            }
        } else if ("compatibility".equals(active)) {
            // The Go gate's historical label says only "not paused". It is
            // neither a configured switch nor proof that a pool exists.
            Map<?, ?> actual = state.get("actual") instanceof Map ? (Map<?, ?>) state.get("actual") : Collections.emptyMap();
            Object zramEnabled = field(actual.get("zram"), "efficiencyEnabled");
            Object ksmEnabled = field(actual.get("ksm"), "enabled");
            boolean nativeKsm = nativeKsmProof(out, state, now);
            String observed = nativeKsm ? (Boolean.TRUE.equals(ksmEnabled) ? "Active" : "Disabled")
                    : controlledPool && (Boolean.TRUE.equals(zramEnabled) || Boolean.TRUE.equals(ksmEnabled))
                    ? "Active" : controlledPool && Boolean.FALSE.equals(zramEnabled) && Boolean.FALSE.equals(ksmEnabled)
                    ? "Disabled" : "Unknown";
            out.setFeatureState(observed);
            out.setFeatureStateSource("Unknown".equals(observed) ? null : nativeKsm ? "KSM_NATIVE" : "GO_LIFECYCLE");
        } else if (active instanceof String) {
            out.setFeatureState((String) active); out.setFeatureStateSource("GO_LIFECYCLE");
        } else if (nativeKsmProof(out, state, now)) {
            Map<?, ?> ksm = (Map<?, ?>) ((Map<?, ?>) state.get("actual")).get("ksm");
            out.setFeatureState(Boolean.TRUE.equals(ksm.get("enabled")) ? "Active" : "Disabled");
            out.setFeatureStateSource("KSM_NATIVE");
        } else { out.setFeatureState("Unknown"); out.setFeatureStateSource(null); }
        java.util.List<String> actions = new java.util.ArrayList<>();
        actions.add("query");
        if (controlledPool) {
            if (Boolean.FALSE.equals(field(lifecycle, "paused"))) actions.add("pause");
            actions.add("drain");
        }
        // These flags come from the repository's task/control proof, but keep
        // the shared projection fail-closed if a caller supplies stale flags.
        boolean taskIdle = out.getActiveTaskUuid() == null && "Succeeded".equals(out.getStatus());
        boolean hasControl = out.getControlOperationUuid() != null;
        if (!contradicted && canResume && taskIdle && hasControl) { actions.add("resume"); }
        out.setAllowedActions(actions);
        Object savings = state.get("savings");
        Map<String, Object> metrics = savings instanceof Map
                ? new java.util.LinkedHashMap<>((Map<String, Object>) savings) : new java.util.LinkedHashMap<>();
        // Presentation metadata is not a new monitoring sample. Clients use
        // the server's configured TTL and elapsed time since this response,
        // without trusting client/server clocks to be synchronized.
        if (savings instanceof Map) {
            metrics.put("displayTtlMillis", MemoryOptimizationGlobalConfig.displayTtlMillis());
            metrics.put("presentationTime", now);
        }
        out.setMetrics(metrics);
        Object quality = metrics.get("quality");
        if (sample == null || sample > now + 5000) out.setQuality("Unknown");
        else if (now - sample >= MemoryOptimizationGlobalConfig.displayTtlMillis()) out.setQuality("Stale");
        else out.setQuality(quality instanceof String ? (String) quality : "Unknown");
    }

    private static Object field(Object object, String name) {
        return object instanceof Map ? ((Map<?, ?>) object).get(name) : null;
    }

    /** A later partial read can invalidate a control proof, but cannot renew it. */
    private static boolean newerPartialContradictsState(MemoryStateInventory out, Map<String, Object> state, long now) {
        Object partial = state.get("nativeKsmObservation");
        if (partial == null) return false;
        Long sampled = exactLong(field(partial, "sampleTime"));
        if (sampled == null) return true;
        if (out.getLastSampleTime() != null && sampled <= out.getLastSampleTime()) return false;
        if (!"KSM_NATIVE_PARTIAL".equals(field(partial, "observationKind")) || sampled > now
                || !java.util.Objects.equals(state.get("bootId"), field(partial, "bootId"))) return true;
        Object observed = field(partial, "ksm"), confirmed = field(state.get("actual"), "ksm");
        Object enabled = field(observed, "enabled");
        if (!(enabled instanceof Boolean) || !enabled.equals(field(confirmed, "enabled"))) return true;
        for (String name : new String[]{"pagesToScan", "sleepMillis"}) {
            Long value = exactLong(field(observed, name));
            if (value == null || !value.equals(exactLong(field(confirmed, name)))) return true;
        }
        Object zeroPages = field(observed, "zeroPagesEnabled");
        return zeroPages != null && (!(zeroPages instanceof Boolean)
                || !zeroPages.equals(field(confirmed, "zeroPagesEnabled")));
    }

    /** UI eligibility only; every mutation still passes normal API admission. */
    private static boolean controlledGoPool(MemoryStateInventory out, Map<String, Object> state, long now) {
        if (!Boolean.TRUE.equals(state.get("managed")) || Boolean.TRUE.equals(state.get("ksmOnly"))
                || !"Succeeded".equals(out.getStatus()) || out.getActiveTaskUuid() != null
                || out.getControlOperationUuid() == null
                || !out.getControlOperationUuid().equals(state.get("lastConfirmedOperationUuid"))
                || out.getAppliedRevision() == null || out.getDesiredRevision() != out.getAppliedRevision()
                || !out.getAppliedRevision().equals(exactLong(state.get("lastConfirmedAppliedRevision")))
                || out.getLastSampleTime() == null || out.getLastSampleTime() > now + 5000
                || now - out.getLastSampleTime() >= MemoryOptimizationGlobalConfig.displayTtlMillis()
                || !(state.get("bootId") instanceof String) || ((String) state.get("bootId")).trim().isEmpty()) return false;
        Object lifecycle = state.get("lifecycle");
        Object paused = field(lifecycle, "paused"), active = field(lifecycle, "activeState");
        if (field(lifecycle, "migrationHold") != null || Boolean.TRUE.equals(state.get("maintenance_blocks_activation"))) return false;
        if (!(Boolean.TRUE.equals(paused) && "paused".equals(active))
                && !(Boolean.FALSE.equals(paused) && ("compatibility".equals(active) || "active".equalsIgnoreCase(String.valueOf(active))))) return false;
        Object pool = state.get("host_zram"), generation = field(pool, "pool_generation");
        if (!(generation instanceof String) || ((String) generation).isEmpty()
                || !generation.equals(state.get("poolGeneration"))
                || !"native_host_device".equals(field(pool, "quality"))
                || !(field(pool, "device") instanceof String)
                || !((String) field(pool, "device")).matches("/dev/zram[0-9]+")) return false;
        try {
            // A fresh Agent envelope must not renew an old native observation.
            long sampled = java.time.OffsetDateTime.parse((String) field(pool, "observed_at")).toInstant().toEpochMilli();
            return sampled <= now + 5000 && now - sampled < MemoryOptimizationGlobalConfig.displayTtlMillis();
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean nativeKsmProof(MemoryStateInventory out, Map<String, Object> state, long now) {
        if (!Boolean.TRUE.equals(state.get("managed")) || !Boolean.TRUE.equals(state.get("ksmOnly"))
                || !"Succeeded".equals(out.getStatus()) || out.getActiveTaskUuid() != null
                || out.getControlOperationUuid() == null
                || out.getAppliedRevision() == null || out.getDesiredRevision() != out.getAppliedRevision()
                || out.getLastSampleTime() == null || out.getLastSampleTime() > now + 5000
                || now - out.getLastSampleTime() >= MemoryOptimizationGlobalConfig.displayTtlMillis()) return false;
        Object lifecycle = state.get("lifecycle");
        if (lifecycle != null) {
            // A reachable observer is not a pool. Accept only its separately
            // confirmed native KSM journal; never grant pool control actions.
            if (!"observe".equals(state.get("mode")) || !Boolean.FALSE.equals(state.get("resource_authorized"))
                    || !"compatibility".equals(field(lifecycle, "activeState"))
                    || !Boolean.FALSE.equals(field(lifecycle, "paused"))
                    || field(lifecycle, "migrationHold") != null
                    || Boolean.TRUE.equals(state.get("maintenance_blocks_activation"))
                    || !out.getControlOperationUuid().equals(state.get("lastConfirmedOperationUuid"))
                    || !out.getAppliedRevision().equals(exactLong(state.get("lastConfirmedAppliedRevision")))
                    || !"unavailable".equals(field(state.get("host_zram"), "quality"))
                    || !"".equals(field(state.get("host_zram"), "pool_generation"))
                    || !"".equals(field(state.get("host_zram"), "device"))) return false;
        }
        Object boot = state.get("bootId"), actual = state.get("actual");
        if (!(boot instanceof String) || ((String) boot).isEmpty() || !(actual instanceof Map)) return false;
        Object k = ((Map<?, ?>) actual).get("ksm"); if (!(k instanceof Map)) return false;
        Map<?, ?> ksm = (Map<?, ?>) k;
        if (!(ksm.get("enabled") instanceof Boolean) || !(ksm.get("zeroPagesEnabled") instanceof Boolean)) return false;
        Long pages = exactLong(ksm.get("pagesToScan")), sleep = exactLong(ksm.get("sleepMillis"));
        return pages != null && pages >= 1 && pages <= 4294967295L
                && sleep != null && sleep >= 10 && sleep <= 4294967295L;
    }
    private static Long exactLong(Object value) {
        if (!(value instanceof Number)) return null;
        try { return new BigDecimal(String.valueOf(value)).longValueExact(); }
        catch (RuntimeException e) { return null; }
    }
}
