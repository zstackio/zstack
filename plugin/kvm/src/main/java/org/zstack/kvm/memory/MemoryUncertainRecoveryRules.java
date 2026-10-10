package org.zstack.kvm.memory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.zstack.utils.gson.JSONObjectUtil;

/** Retire a missing-receipt maintenance activation, never invent its historical outcome. */
public final class MemoryUncertainRecoveryRules {
    public static final String ACTION = "recoverUncertain";
    private MemoryUncertainRecoveryRules() { }

    public static void validate(APIUpdateMemoryPolicyMsg msg) {
        MemoryUncertainRecovery request = msg.getRecovery();
        if (!ACTION.equals(msg.getAction())) {
            if (request != null) { invalid(); }
            return;
        }
        if (!"Host".equals(msg.getScope()) || msg.getTargetHostUuids() != null
                || !MemoryTaskRules.isSafetyAction("pause", msg.getPolicy())
                || request == null || !request.isConfirmed()
                || !matches(request.getExpectedHostBootId(),
                    "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
                || !matches(request.getExpectedPoolGeneration(), "[a-f0-9]{64}")
                || !matches(request.getDrainControlOperationUuid(), "[a-f0-9]{32}")
                || !matches(msg.getExpectedControlOperationUuid(), "[a-f0-9]{32}")) { invalid(); }
    }

    /** Correlate the new recovery receipt, the original Unknown task and the exact drain. */
    static boolean confirms(MemoryTaskVO recovery, MemoryTaskVO original, MemoryTaskVO drain,
                            MemoryStateVO host, MemoryAgentResponse response) {
        if (recovery == null || original == null || drain == null || host == null || response == null
                || !ACTION.equals(recovery.getAction()) || !"Unknown".equals(original.getStatus())
                || !Arrays.asList("apply", "clearOverride").contains(original.getAction())
                || !"Host".equals(original.getScope())
                || !Objects.equals(recovery.getHostUuid(), original.getHostUuid())
                || !Objects.equals(recovery.getHostUuid(), drain.getHostUuid())
                || !Objects.equals(recovery.getHostUuid(), host.getHostUuid())
                || !Objects.equals(recovery.getExpectedControlOperationUuid(), original.getUuid())
                || !Objects.equals(host.getControlOperationUuid(), original.getUuid())
                || !"drain".equals(drain.getAction()) || !"Succeeded".equals(drain.getStatus())
                || !response.isSuccess() || !"Succeeded".equals(response.status)
                || response.appliedRevision != null || response.state == null
                || !Objects.equals(response.operationUuid, recovery.getUuid())
                || !"UNCERTAIN_RECOVERED".equals(response.state.get("phase"))) { return false; }
        try {
            Map<?, ?> request = JSONObjectUtil.toObject(recovery.getPolicy(), Map.class);
            Map<?, ?> previous = JSONObjectUtil.toObject(host.getState(), Map.class);
            Object value = response.state.get("recoveryProof");
            Object maintenance = response.state.get("maintenanceProof");
            if (!(value instanceof Map) || !(maintenance instanceof Map)) { return false; }
            Map<?, ?> proof = (Map<?, ?>) value;
            Map<?, ?> ready = (Map<?, ?>) maintenance;
            return Objects.equals(request.get("expectedHostBootId"), response.bootId)
                    && Objects.equals(previous.get("bootId"), response.bootId)
                    && Objects.equals(request.get("drainControlOperationUuid"), drain.getUuid())
                    && Objects.equals(response.state.get("lastConfirmedOperationUuid"), drain.getUuid())
                    && proof.get("schemaVersion") instanceof Number
                    && ((Number) proof.get("schemaVersion")).doubleValue() == 1D
                    && Objects.equals(proof.get("hostUuid"), host.getHostUuid())
                    && Objects.equals(proof.get("bootId"), response.bootId)
                    && Objects.equals(proof.get("recoveryOperationUuid"), recovery.getUuid())
                    && Objects.equals(proof.get("unresolvedOperationUuid"), original.getUuid())
                    && Objects.equals(proof.get("drainControlOperationUuid"), drain.getUuid())
                    && Objects.equals(proof.get("expectedPoolGeneration"), request.get("expectedPoolGeneration"))
                    && Objects.equals(proof.get("oldActivationRequestId"), activationId(original.getUuid()))
                    && Objects.equals(proof.get("oldActivationFingerprint"), activationFingerprint(original.getUuid(), request))
                    && Boolean.TRUE.equals(proof.get("retired"))
                    && "Unknown".equals(proof.get("originalOutcome"))
                    && Objects.equals(ready.get("oldPoolGeneration"), request.get("expectedPoolGeneration"))
                    && Boolean.TRUE.equals(ready.get("maintenanceReady"))
                    && Boolean.TRUE.equals(ready.get("maintenanceArchived"))
                    && Boolean.TRUE.equals(ready.get("executorExited"))
                    && Boolean.TRUE.equals(ready.get("readyForInitialization"))
                    && Boolean.TRUE.equals(ready.get("originalDeviceInactive"))
                    && Boolean.TRUE.equals(ready.get("oldPoolOwnershipAbsent"))
                    && ready.get("activeOperations") instanceof Number
                    && ((Number) ready.get("activeOperations")).doubleValue() == 0D
                    && MemoryRepository.isMaintenanceRecoveryState(JSONObjectUtil.toJsonString(response.state), drain.getUuid());
        } catch (RuntimeException ignored) { return false; }
    }

    /** A durable rejection decides only this new, unissued recovery request. */
    static boolean confirmsNotIssued(MemoryTaskVO recovery, MemoryTaskVO original, MemoryTaskVO drain,
                                     MemoryStateVO host, MemoryAgentResponse response) {
        if (recovery == null || original == null || drain == null || host == null || response == null
                || !ACTION.equals(recovery.getAction()) || !"Host".equals(recovery.getScope())
                || !"Unknown".equals(original.getStatus()) || !"Host".equals(original.getScope())
                || !Arrays.asList("apply", "clearOverride").contains(original.getAction())
                || !Objects.equals(recovery.getHostUuid(), original.getHostUuid())
                || !Objects.equals(recovery.getHostUuid(), drain.getHostUuid())
                || !Objects.equals(recovery.getHostUuid(), host.getHostUuid())
                || !Objects.equals(recovery.getExpectedControlOperationUuid(), original.getUuid())
                || !Objects.equals(host.getControlOperationUuid(), original.getUuid())
                || !"drain".equals(drain.getAction()) || !"Succeeded".equals(drain.getStatus())
                || response.isSuccess() || !"Failed".equals(response.status)
                || response.appliedRevision != null || response.state == null
                || !Objects.equals(response.operationUuid, recovery.getUuid())
                || !"RECOVERY_NOT_ISSUED".equals(response.state.get("phase"))
                || response.state.containsKey("retirementReceipt") || response.state.containsKey("recoveryProof")) { return false; }
        try {
            Map<?, ?> request = JSONObjectUtil.toObject(recovery.getPolicy(), Map.class);
            Map<?, ?> previous = JSONObjectUtil.toObject(host.getState(), Map.class);
            Object value = response.state.get("recoveryRejectionProof");
            if (!(value instanceof Map)) { return false; }
            Map<?, ?> proof = (Map<?, ?>) value;
            return Objects.equals(request.get("expectedHostBootId"), response.bootId)
                    && Objects.equals(previous.get("bootId"), response.bootId)
                    && Objects.equals(response.state.get("bootId"), response.bootId)
                    && Objects.equals(request.get("drainControlOperationUuid"), drain.getUuid())
                    && proof.get("schemaVersion") instanceof Number
                    && ((Number) proof.get("schemaVersion")).doubleValue() == 1D
                    && Objects.equals(proof.get("hostUuid"), host.getHostUuid())
                    && Objects.equals(proof.get("bootId"), response.bootId)
                    && Objects.equals(proof.get("recoveryOperationUuid"), recovery.getUuid())
                    && Objects.equals(proof.get("unresolvedOperationUuid"), original.getUuid())
                    && Objects.equals(proof.get("drainControlOperationUuid"), drain.getUuid())
                    && Objects.equals(proof.get("expectedPoolGeneration"), request.get("expectedPoolGeneration"))
                    && Boolean.TRUE.equals(proof.get("notIssued"));
        } catch (RuntimeException ignored) { return false; }
    }

    static String activationId(String operation) { return sha256(operation + ":maintenance-activation").substring(0, 32); }

    static String activationFingerprint(String operation, Map<?, ?> request) {
        // Match the existing Agent OrderedDict / Go struct JSON wire order.
        Map<String, Object> intent = new java.util.LinkedHashMap<>();
        intent.put("request_id", activationId(operation));
        intent.put("expected_control_operation_uuid", request.get("drainControlOperationUuid"));
        intent.put("expected_host_boot_id", request.get("expectedHostBootId"));
        intent.put("expected_pool_generation", request.get("expectedPoolGeneration"));
        return sha256(new com.google.gson.Gson().toJson(intent));
    }

    private static String sha256(String value) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) { result.append(String.format("%02x", b & 0xff)); }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static boolean matches(Object value, String pattern) {
        return value instanceof String && ((String) value).matches(pattern);
    }

    private static void invalid() {
        throw new MemoryOperationException("MEMORY_UNCERTAIN_RECOVERY_INVALID",
                "Explicit Host recovery requires the current Unknown operation, boot, drained pool generation, drain operation and confirmation");
    }
}
