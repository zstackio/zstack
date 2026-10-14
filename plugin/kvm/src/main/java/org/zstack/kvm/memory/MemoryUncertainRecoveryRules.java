package org.zstack.kvm.memory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.zstack.utils.gson.JSONObjectUtil;

/** Recover only a proven final step, never invent an uncertain historical outcome. */
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
                || (request.getDrainControlOperationUuid() != null
                    && !matches(request.getDrainControlOperationUuid(), "[a-f0-9]{32}"))
                || !matches(msg.getExpectedControlOperationUuid(), "[a-f0-9]{32}")) { invalid(); }
    }

    /** A tuning-only apply may restore its original CPU gate without replaying tuning. */
    static boolean confirmsPolicy(MemoryTaskVO recovery, MemoryTaskVO original,
                                  MemoryStateVO host, MemoryAgentResponse response) {
        if (recovery == null || original == null || host == null || response == null
                || !ACTION.equals(recovery.getAction()) || !"Host".equals(recovery.getScope())
                || !"Unknown".equals(original.getStatus()) || !"apply".equals(original.getAction())
                || !"Host".equals(original.getScope())
                || !Objects.equals(recovery.getHostUuid(), original.getHostUuid())
                || !Objects.equals(recovery.getHostUuid(), host.getHostUuid())
                || !Objects.equals(recovery.getExpectedControlOperationUuid(), original.getUuid())
                || !Objects.equals(host.getControlOperationUuid(), original.getUuid())
                || recovery.getDesiredRevision() != original.getDesiredRevision()
                || host.getDesiredRevision() != original.getDesiredRevision()
                || (host.getAppliedRevision() != null && host.getAppliedRevision() > original.getDesiredRevision())
                || !response.isSuccess() || !"Succeeded".equals(response.status)
                || !Objects.equals(response.operationUuid, recovery.getUuid())
                || response.appliedRevision == null || response.appliedRevision != original.getDesiredRevision()
                || response.state == null || !"POLICY_RECOVERED".equals(response.state.get("phase"))
                || response.sampleTime == null || response.sampleTime > System.currentTimeMillis()
                || System.currentTimeMillis() - response.sampleTime >= MemoryOptimizationGlobalConfig.displayTtlMillis()
                || (host.getLastSampleTime() != null && response.sampleTime < host.getLastSampleTime())) { return false; }
        try {
            Map<?, ?> request = JSONObjectUtil.toObject(recovery.getPolicy(), Map.class);
            Map<?, ?> previous = JSONObjectUtil.toObject(host.getState(), Map.class);
            Map<?, ?> proof = (Map<?, ?>) response.state.get("recoveryProof");
            Map<?, ?> before = (Map<?, ?>) response.state.get("cpuBefore");
            Map<?, ?> receipt = (Map<?, ?>) response.state.get("cpuReceipt");
            Map<?, ?> readback = (Map<?, ?>) response.state.get("cpuReadback");
            if (proof == null || before == null || receipt == null || readback == null
                    || request.get("drainControlOperationUuid") != null
                    || !Boolean.TRUE.equals(request.get("confirmed"))) { return false; }
            long beforeRevision = new java.math.BigDecimal(before.get("revision").toString()).longValueExact();
            long expectedRevision = new java.math.BigDecimal(proof.get("cpuExpectedRevision").toString()).longValueExact();
            if (beforeRevision < 0 || expectedRevision != Math.addExact(beforeRevision, 1L)
                    || !Boolean.TRUE.equals(before.get("efficiency_enabled"))) { return false; }
            for (String key : Arrays.asList("efficiency_enabled", "idle_cpu_threshold_percent", "idle_duration_seconds")) {
                if (before.get(key) == null || !Objects.equals(before.get(key), receipt.get(key))
                        || !Objects.equals(before.get(key), readback.get(key))) { return false; }
            }
            return Objects.equals(request.get("expectedHostBootId"), response.bootId)
                    && Objects.equals(previous.get("bootId"), response.bootId)
                    && Objects.equals(response.state.get("bootId"), response.bootId)
                    && Objects.equals(response.state.get("lastConfirmedOperationUuid"), recovery.getUuid())
                    && numberEquals(response.state.get("lastConfirmedAppliedRevision"), original.getDesiredRevision())
                    && numberEquals(proof.get("schemaVersion"), 1)
                    && Objects.equals(proof.get("hostUuid"), host.getHostUuid())
                    && Objects.equals(proof.get("bootId"), response.bootId)
                    && Objects.equals(proof.get("recoveryOperationUuid"), recovery.getUuid())
                    && Objects.equals(proof.get("unresolvedOperationUuid"), original.getUuid())
                    && Objects.equals(proof.get("expectedPoolGeneration"), request.get("expectedPoolGeneration"))
                    && matches(proof.get("originalRequestHash"), "[a-f0-9]{64}")
                    && Objects.equals(proof.get("originalPolicyHash"), sha256(
                            MemoryPolicyRules.canonicalRequest(MemoryPolicyRules.decode(original.getPolicy()))))
                    && numberEquals(proof.get("originalDesiredRevision"), original.getDesiredRevision())
                    && proof.get("tuningRevision") instanceof Number
                    && new java.math.BigDecimal(proof.get("tuningRevision").toString()).longValueExact() >= 0
                    && Boolean.TRUE.equals(proof.get("finalCpuRestored"))
                    && "Unknown".equals(proof.get("originalOutcome"))
                    && Objects.equals(receipt.get("request_id"), original.getUuid() + "-cpu")
                    && numberEquals(receipt.get("revision"), Math.addExact(expectedRevision, 1L))
                    && numberEquals(readback.get("revision"), Math.addExact(expectedRevision, 1L));
        } catch (RuntimeException ignored) { return false; }
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
        if (recovery == null || original == null || host == null || response == null
                || !ACTION.equals(recovery.getAction()) || !"Host".equals(recovery.getScope())
                || !"Unknown".equals(original.getStatus()) || !"Host".equals(original.getScope())
                || !Arrays.asList("apply", "clearOverride").contains(original.getAction())
                || !Objects.equals(recovery.getHostUuid(), original.getHostUuid())
                || (drain != null && !Objects.equals(recovery.getHostUuid(), drain.getHostUuid()))
                || !Objects.equals(recovery.getHostUuid(), host.getHostUuid())
                || !Objects.equals(recovery.getExpectedControlOperationUuid(), original.getUuid())
                || !Objects.equals(host.getControlOperationUuid(), original.getUuid())
                || (drain != null && (!"drain".equals(drain.getAction()) || !"Succeeded".equals(drain.getStatus())))
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
            String drainUuid = drain == null ? null : drain.getUuid();
            if (drain == null && !"apply".equals(original.getAction())) { return false; }
            return Objects.equals(request.get("expectedHostBootId"), response.bootId)
                    && Objects.equals(previous.get("bootId"), response.bootId)
                    && Objects.equals(response.state.get("bootId"), response.bootId)
                    && Objects.equals(request.get("drainControlOperationUuid"), drainUuid)
                    && proof.get("schemaVersion") instanceof Number
                    && ((Number) proof.get("schemaVersion")).doubleValue() == 1D
                    && Objects.equals(proof.get("hostUuid"), host.getHostUuid())
                    && Objects.equals(proof.get("bootId"), response.bootId)
                    && Objects.equals(proof.get("recoveryOperationUuid"), recovery.getUuid())
                    && Objects.equals(proof.get("unresolvedOperationUuid"), original.getUuid())
                    && Objects.equals(proof.get("drainControlOperationUuid"), drainUuid)
                    && Objects.equals(proof.get("expectedPoolGeneration"), request.get("expectedPoolGeneration"))
                    && Boolean.TRUE.equals(proof.get("notIssued"));
        } catch (RuntimeException ignored) { return false; }
    }

    private static boolean numberEquals(Object value, long expected) {
        if (!(value instanceof Number)) { return false; }
        try { return new java.math.BigDecimal(value.toString()).longValueExact() == expected; }
        catch (ArithmeticException | NumberFormatException ignored) { return false; }
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
                "Explicit Host recovery requires the current Unknown operation, boot, pool generation and confirmation; activation recovery also requires its exact drain operation");
    }
}
