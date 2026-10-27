package org.zstack.kvm.memory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.zstack.utils.gson.JSONObjectUtil;

/** Recover only a proven final step, never invent an uncertain historical outcome. */
public final class MemoryUncertainRecoveryRules {
    public static final String ACTION = "recoverUncertain";
    private MemoryUncertainRecoveryRules() { }

    public static void validate(MemoryActionRequest msg) {
        MemoryUncertainRecovery request = msg.getRecovery();
        if (!ACTION.equals(msg.getAction())) {
            if (request != null) { invalid(); }
            return;
        }
        if (!"Host".equals(msg.getScope())
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
        return confirmsPolicy(recovery, original, null, host, response);
    }

    static boolean confirmsPolicy(MemoryTaskVO recovery, MemoryTaskVO original, MemoryTaskVO anchor,
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
                    || !Boolean.TRUE.equals(request.get("confirmed"))
                    || !confirmsPolicyProofIdentity(recovery, original, host, response, request, previous, proof)
                    || !Boolean.TRUE.equals(proof.get("finalCpuRestored"))
                    || !confirmsFinalCpu(original, proof, before, receipt, readback)) { return false; }
            Object proofKind = proof.get("kind");
            if ("partialServiceApply".equals(proofKind)) {
                return confirmsPartialServicePolicy(original, anchor, host, response, request, proof, before);
            }
            if (proofKind != null || containsPartialServiceProof(proof)
                    || isPartialServicePolicy(original.getPolicy())) { return false; }
            return proof.get("tuningRevision") instanceof Number
                    && new java.math.BigDecimal(proof.get("tuningRevision").toString()).longValueExact() >= 0;
        } catch (RuntimeException ignored) { return false; }
    }

    private static boolean confirmsPolicyProofIdentity(MemoryTaskVO recovery, MemoryTaskVO original,
            MemoryStateVO host, MemoryAgentResponse response, Map<?, ?> request, Map<?, ?> previous,
            Map<?, ?> proof) {
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
                && "Unknown".equals(proof.get("originalOutcome"));
    }

    private static boolean confirmsPartialServicePolicy(MemoryTaskVO original, MemoryTaskVO anchor,
            MemoryStateVO host, MemoryAgentResponse response, Map<?, ?> request, Map<?, ?> proof,
            Map<?, ?> cpuBefore) {
        Map<?, ?> originalKsm = asMap(proof.get("originalKsmReadback"));
        Map<?, ?> cpuGuard = asMap(proof.get("cpuGuard"));
        Map<?, ?> cpuGuardReadback = asMap(proof.get("cpuGuardReadback"));
        Map<?, ?> tuningBefore = asMap(proof.get("tuningBefore"));
        Map<?, ?> tuningCommand = asMap(proof.get("tuningCommand"));
        Map<?, ?> tuningReceipt = asMap(proof.get("tuningReceipt"));
        Map<?, ?> tuningReadback = asMap(proof.get("tuningReadback"));
        if (!isPartialServicePolicy(original.getPolicy()) || !validPartialAnchor(anchor, original, host, proof)
                || request.get("drainControlOperationUuid") != null
                || !Boolean.TRUE.equals(request.get("confirmed"))
                || (response.hostUuid != null && !Objects.equals(response.hostUuid, host.getHostUuid()))
                || !matches(proof.get("originalJournalHash"), "[a-f0-9]{64}")
                || !validOriginalKsmReadback(original, originalKsm)
                || !validCpuGuard(original, proof, cpuBefore, cpuGuard, cpuGuardReadback)
                || !validPartialTuning(original, proof, tuningBefore, tuningCommand,
                        tuningReceipt, tuningReadback)) { return false; }
        return true;
    }

    private static boolean containsPartialServiceProof(Map<?, ?> proof) {
        for (String key : Arrays.asList("anchorOperationUuid", "anchorJournalHash", "originalJournalHash",
                "originalKsmReadback", "cpuGuard", "cpuGuardReadback", "tuningBefore", "tuningCommand",
                "tuningRequestId", "tuningExpectedRevision", "tuningPayloadSha256", "tuningReceipt", "tuningReadback")) {
            if (proof.containsKey(key)) { return true; }
        }
        return false;
    }

    private static boolean validPartialAnchor(MemoryTaskVO anchor, MemoryTaskVO original,
            MemoryStateVO host, Map<?, ?> proof) {
        if (anchor == null || !Objects.equals(proof.get("anchorOperationUuid"), anchor.getUuid())
                || !matches(anchor.getUuid(), "[a-fA-F0-9]{32}")
                || !matches(proof.get("anchorJournalHash"), "[a-f0-9]{64}")
                || !"apply".equals(anchor.getAction()) || !"Succeeded".equals(anchor.getStatus())
                || !"Host".equals(anchor.getScope())
                || !Objects.equals(anchor.getHostUuid(), original.getHostUuid())
                || !Objects.equals(anchor.getHostUuid(), host.getHostUuid())
                || !Objects.equals(anchor.getResourceUuid(), host.getHostUuid())
                || anchor.getDesiredRevision() < 0 || anchor.getDesiredRevision() >= original.getDesiredRevision()
                || host.getAppliedRevision() == null || host.getAppliedRevision() != anchor.getDesiredRevision()) {
            return false;
        }
        MemoryPolicyConfig anchorPolicy = MemoryPolicyRules.decode(anchor.getPolicy());
        return anchorPolicy != null && anchorPolicy.ksm != null
                && Boolean.FALSE.equals(anchorPolicy.ksm.enabled)
                && Boolean.FALSE.equals(anchorPolicy.ksm.zeroPagesEnabled)
                && anchorPolicy.writeback != null && Boolean.TRUE.equals(anchorPolicy.writeback.enabled)
                && MemoryPolicyRules.validUuid(anchorPolicy.writeback.backendResourceUuid)
                && anchorPolicy.writeback.backendCapacityBytes != null
                && anchorPolicy.writeback.backendCapacityBytes > 0;
    }

    private static boolean validOriginalKsmReadback(MemoryTaskVO original, Map<?, ?> readback) {
        if (readback == null || !hasExactKeys(readback, "configRevision", "run", "use_zero_pages",
                "pages_to_scan", "sleep_millisecs")
                || !Objects.equals(readback.get("configRevision"), String.valueOf(original.getDesiredRevision()))
                || !"1".equals(readback.get("run")) || !"1".equals(readback.get("use_zero_pages"))) {
            return false;
        }
        try {
            long pages = Long.parseLong((String) readback.get("pages_to_scan"));
            long sleep = Long.parseLong((String) readback.get("sleep_millisecs"));
            return pages >= 1 && pages <= 0xffffffffL && sleep >= 10 && sleep <= 0xffffffffL;
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean validCpuGuard(MemoryTaskVO original, Map<?, ?> proof, Map<?, ?> before,
            Map<?, ?> guard, Map<?, ?> readback) {
        if (guard == null || readback == null || !numberEquals(guard.get("_http_status"), 200)
                || !numberEquals(readback.get("_http_status"), 200)) { return false; }
        try {
            long beforeRevision = new java.math.BigDecimal(before.get("revision").toString()).longValueExact();
            long expected = Math.addExact(beforeRevision, 1L);
            return numberEquals(proof.get("cpuExpectedRevision"), expected)
                    && numberEquals(guard.get("revision"), expected)
                    && numberEquals(readback.get("revision"), expected)
                    && Objects.equals(guard.get("request_id"), original.getUuid() + "-cpu-hold")
                    && Boolean.TRUE.equals(before.get("efficiency_enabled"))
                    && Boolean.FALSE.equals(guard.get("efficiency_enabled"))
                    && Objects.equals(guard.get("idle_cpu_threshold_percent"), before.get("idle_cpu_threshold_percent"))
                    && Objects.equals(guard.get("idle_duration_seconds"), before.get("idle_duration_seconds"))
                    && Boolean.FALSE.equals(readback.get("efficiency_enabled"))
                    && Objects.equals(readback.get("idle_cpu_threshold_percent"), before.get("idle_cpu_threshold_percent"))
                    && Objects.equals(readback.get("idle_duration_seconds"), before.get("idle_duration_seconds"))
                    && Objects.equals(guard.get("efficiency_enabled"), readback.get("efficiency_enabled"))
                    && Objects.equals(guard.get("idle_cpu_threshold_percent"), readback.get("idle_cpu_threshold_percent"))
                    && Objects.equals(guard.get("idle_duration_seconds"), readback.get("idle_duration_seconds"));
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean confirmsFinalCpu(MemoryTaskVO original, Map<?, ?> proof, Map<?, ?> before,
            Map<?, ?> receipt, Map<?, ?> readback) {
        if (!numberEquals(receipt.get("_http_status"), 200)
                || !numberEquals(readback.get("_http_status"), 200)) { return false; }
        try {
            long beforeRevision = new java.math.BigDecimal(before.get("revision").toString()).longValueExact();
            long expectedRevision = new java.math.BigDecimal(proof.get("cpuExpectedRevision").toString()).longValueExact();
            if (beforeRevision < 0 || expectedRevision != Math.addExact(beforeRevision, 1L)
                    || !Boolean.TRUE.equals(before.get("efficiency_enabled"))) { return false; }
            for (String key : Arrays.asList("efficiency_enabled", "idle_cpu_threshold_percent", "idle_duration_seconds")) {
                if (before.get(key) == null || !Objects.equals(before.get(key), receipt.get(key))
                        || !Objects.equals(before.get(key), readback.get(key))) { return false; }
            }
            return Objects.equals(receipt.get("request_id"), original.getUuid() + "-cpu")
                    && numberEquals(receipt.get("revision"), Math.addExact(expectedRevision, 1L))
                    && numberEquals(readback.get("revision"), Math.addExact(expectedRevision, 1L));
        } catch (RuntimeException ignored) { return false; }
    }

    private static boolean validPartialTuning(MemoryTaskVO original, Map<?, ?> proof,
            Map<?, ?> before, Map<?, ?> command, Map<?, ?> receipt, Map<?, ?> readback) {
        if (before == null || command == null || receipt == null || readback == null
                || !numberEquals(before.get("_http_status"), 200)
                || !numberEquals(receipt.get("_http_status"), 200)
                || !numberEquals(readback.get("_http_status"), 200)
                || !hasExactKeys(command, "request_id", "expected_revision", "settings")) { return false; }
        try {
            long beforeRevision = new java.math.BigDecimal(before.get("revision").toString()).longValueExact();
            long receiptRevision = Math.addExact(beforeRevision, 1L);
            String requestId = original.getUuid() + "-tuning";
            Map<?, ?> beforeSettings = asMap(before.get("settings"));
            Map<?, ?> beforeWriteback = beforeSettings == null ? null : asMap(beforeSettings.get("writeback"));
            if (beforeRevision < 0 || beforeWriteback == null || !Boolean.TRUE.equals(beforeWriteback.get("enabled"))
                    || !Objects.equals(proof.get("tuningRequestId"), requestId)
                    || !Objects.equals(command.get("request_id"), requestId)
                    || !numberEquals(proof.get("tuningExpectedRevision"), beforeRevision)
                    || !numberEquals(command.get("expected_revision"), beforeRevision)
                    || !numberEquals(proof.get("tuningRevision"), receiptRevision)
                    || !numberEquals(receipt.get("revision"), receiptRevision)
                    || !numberEquals(readback.get("revision"), receiptRevision)
                    || !Objects.equals(receipt.get("agent_request_id"), requestId)
                    || !numberEquals(receipt.get("agent_expected_revision"), beforeRevision)) { return false; }
            @SuppressWarnings("unchecked")
            Map<String, Object> targetSettings = (Map<String, Object>) asMap(JSONObjectUtil.toObject(
                    MemoryPolicyRules.canonicalRequest(beforeSettings), Map.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> targetWriteback = (Map<String, Object>) asMap(targetSettings.get("writeback"));
            if (targetWriteback == null) { return false; }
            targetWriteback.put("enabled", false);
            Object commandSettings = command.get("settings");
            if (!Objects.equals(MemoryPolicyRules.canonicalRequest(targetSettings),
                    MemoryPolicyRules.canonicalRequest(commandSettings))
                    || !Objects.equals(MemoryPolicyRules.canonicalRequest(commandSettings),
                        MemoryPolicyRules.canonicalRequest(receipt.get("settings")))
                    || !Objects.equals(MemoryPolicyRules.canonicalRequest(commandSettings),
                        MemoryPolicyRules.canonicalRequest(readback.get("settings")))) { return false; }
            return matches(proof.get("tuningPayloadSha256"), "[a-f0-9]{64}")
                    && Objects.equals(proof.get("tuningPayloadSha256"), sha256(
                            MemoryPolicyRules.canonicalRequest(command)));
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean isPartialServicePolicy(String value) {
        try {
            Map<?, ?> policy = JSONObjectUtil.toObject(value, Map.class);
            Map<?, ?> ksm = asMap(policy.get("ksm"));
            Map<?, ?> writeback = asMap(policy.get("writeback"));
            return hasExactKeys(policy, "schemaVersion", "ksm", "writeback")
                    && numberEquals(policy.get("schemaVersion"), 1)
                    && hasExactKeys(ksm, "enabled", "zeroPagesEnabled")
                    && Boolean.TRUE.equals(ksm.get("enabled"))
                    && Boolean.TRUE.equals(ksm.get("zeroPagesEnabled"))
                    && hasExactKeys(writeback, "enabled")
                    && Boolean.FALSE.equals(writeback.get("enabled"));
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean hasExactKeys(Map<?, ?> map, String... names) {
        if (map == null || map.size() != names.length) { return false; }
        return map.keySet().containsAll(Arrays.asList(names));
    }

    private static Map<?, ?> asMap(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : null;
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
