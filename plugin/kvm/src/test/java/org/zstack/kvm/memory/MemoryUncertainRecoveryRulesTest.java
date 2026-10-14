package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryUncertainRecoveryRulesTest {
    static final String OLD = "11111111111111111111111111111111";
    static final String DRAIN = "22222222222222222222222222222222";
    static final String BOOT = "12345678-1234-1234-1234-123456789abc";
    static final String POOL = String.join("", Collections.nCopies(64, "a"));
    static final String TUNING_POLICY = "{\"schemaVersion\":1,\"writeback\":{\"batchBytes\":67108864}}";

    static Map<String, Object> request() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("expectedHostBootId", BOOT); request.put("expectedPoolGeneration", POOL);
        request.put("drainControlOperationUuid", DRAIN); request.put("confirmed", true);
        return request;
    }

    static MemoryUncertainRecovery requestDto() {
        MemoryUncertainRecovery request = new MemoryUncertainRecovery();
        request.setExpectedHostBootId(BOOT); request.setExpectedPoolGeneration(POOL);
        request.setDrainControlOperationUuid(DRAIN); request.setConfirmed(true);
        return request;
    }

    static MemoryAgentResponse proof(String operation) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = operation; response.status = "Succeeded"; response.setSuccess(true);
        response.bootId = BOOT; response.state = new LinkedHashMap<>();
        response.state.put("phase", "UNCERTAIN_RECOVERED"); response.state.put("bootId", BOOT);
        response.state.put("lastConfirmedOperationUuid", DRAIN);
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("schemaVersion", 1); proof.put("hostUuid", "host1"); proof.put("bootId", BOOT);
        proof.put("recoveryOperationUuid", operation); proof.put("unresolvedOperationUuid", OLD);
        proof.put("drainControlOperationUuid", DRAIN);
        proof.put("oldActivationRequestId", MemoryUncertainRecoveryRules.activationId(OLD));
        proof.put("oldActivationFingerprint", MemoryUncertainRecoveryRules.activationFingerprint(OLD, request()));
        proof.put("expectedPoolGeneration", POOL);
        proof.put("retired", true); proof.put("originalOutcome", "Unknown");
        response.state.put("recoveryProof", proof);
        Map<String, Object> ready = new LinkedHashMap<>();
        ready.put("maintenanceReady", true); ready.put("maintenanceArchived", true);
        ready.put("drainOperationUuid", DRAIN); ready.put("oldPoolGeneration", POOL);
        ready.put("activeOperations", 0); ready.put("executorExited", true);
        ready.put("readyForInitialization", true); ready.put("originalDeviceInactive", true);
        ready.put("oldPoolOwnershipAbsent", true);
        response.state.put("maintenanceProof", ready);
        return response;
    }

    static MemoryAgentResponse notIssued(String operation) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = operation; response.status = "Failed"; response.setSuccess(false);
        response.bootId = BOOT; response.reasonCode = "RECOVERY_MAINTENANCE_PROOF_UNQUALIFIED";
        response.state = new LinkedHashMap<>(); response.state.put("phase", "RECOVERY_NOT_ISSUED");
        response.state.put("bootId", BOOT);
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("schemaVersion", 1); proof.put("hostUuid", "host1"); proof.put("bootId", BOOT);
        proof.put("recoveryOperationUuid", operation); proof.put("unresolvedOperationUuid", OLD);
        proof.put("drainControlOperationUuid", DRAIN); proof.put("expectedPoolGeneration", POOL);
        proof.put("notIssued", true); response.state.put("recoveryRejectionProof", proof);
        return response;
    }

    static MemoryAgentResponse policyProof(String operation) {
        MemoryAgentResponse response = proof(operation);
        response.appliedRevision = 9L; response.sampleTime = System.currentTimeMillis();
        response.state.remove("maintenanceProof");
        response.state.put("phase", "POLICY_RECOVERED");
        response.state.put("lastConfirmedOperationUuid", operation);
        response.state.put("lastConfirmedAppliedRevision", 9L);
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("schemaVersion", 1); proof.put("hostUuid", "host1"); proof.put("bootId", BOOT);
        proof.put("recoveryOperationUuid", operation); proof.put("unresolvedOperationUuid", OLD);
        proof.put("expectedPoolGeneration", POOL); proof.put("originalRequestHash", POOL);
        proof.put("originalPolicyHash", org.apache.commons.codec.digest.DigestUtils.sha256Hex(
                MemoryPolicyRules.canonicalRequest(MemoryPolicyRules.decode(TUNING_POLICY))));
        proof.put("originalDesiredRevision", 9); proof.put("tuningRevision", 6);
        proof.put("cpuExpectedRevision", 13); proof.put("finalCpuRestored", true);
        proof.put("originalOutcome", "Unknown"); response.state.put("recoveryProof", proof);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("revision", 12); before.put("efficiency_enabled", true);
        before.put("idle_cpu_threshold_percent", 5); before.put("idle_duration_seconds", 60);
        response.state.put("cpuBefore", before);
        Map<String, Object> receipt = new LinkedHashMap<>(before);
        receipt.put("revision", 14); receipt.put("request_id", OLD + "-cpu");
        response.state.put("cpuReceipt", receipt);
        Map<String, Object> readback = new LinkedHashMap<>(receipt); readback.remove("request_id");
        response.state.put("cpuReadback", readback);
        return response;
    }

    private APIUpdateMemoryPolicyMsg message() {
        APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
        msg.setScope("Host"); msg.setResourceUuid("host1"); msg.setAction("recoverUncertain");
        msg.setExpectedControlOperationUuid(OLD); msg.setRecovery(requestDto()); return msg;
    }

    @Test public void requiresExplicitNarrowHostRecoveryWithoutPolicyWrites() {
        MemoryUncertainRecoveryRules.validate(message());
        APIUpdateMemoryPolicyMsg ordinary = message();
        ordinary.getRecovery().setDrainControlOperationUuid(null);
        MemoryUncertainRecoveryRules.validate(ordinary);
        APIUpdateMemoryPolicyMsg msg = message(); msg.getRecovery().setConfirmed(false); rejects(msg);
        msg = message(); msg.setAction("apply"); rejects(msg);
        msg = message(); msg.setScope("Global"); rejects(msg);
        msg = message(); msg.setTargetHostUuids(Collections.singletonList("host1")); rejects(msg);
        msg = message(); msg.setPolicy("{\"zram\":{\"enabled\":true}}"); rejects(msg);
        msg = message(); msg.getRecovery().setExpectedPoolGeneration("../path"); rejects(msg);
        assertTrue(MemoryTaskRules.requiresLicenseForConfiguration("recoverUncertain", "{}"));
    }

    @Test public void policyRecoveryRequiresExactOriginalAndCompleteFinalReceipt() {
        MemoryTaskVO recovery = new MemoryTaskVO(); recovery.setUuid("recovery"); recovery.setHostUuid("host1");
        recovery.setScope("Host"); recovery.setAction("recoverUncertain"); recovery.setDesiredRevision(9);
        recovery.setExpectedControlOperationUuid(OLD);
        Map<String, Object> request = request(); request.remove("drainControlOperationUuid");
        recovery.setPolicy(org.zstack.utils.gson.JSONObjectUtil.toJsonString(request));
        MemoryTaskVO old = new MemoryTaskVO(); old.setUuid(OLD); old.setHostUuid("host1"); old.setScope("Host");
        old.setAction("apply"); old.setStatus("Unknown"); old.setDesiredRevision(9); old.setPolicy(TUNING_POLICY);
        MemoryStateVO host = new MemoryStateVO(); host.setHostUuid("host1"); host.setControlOperationUuid(OLD);
        host.setDesiredRevision(9); host.setAppliedRevision(8L); host.setState("{\"bootId\":\"" + BOOT + "\"}");
        assertTrue(MemoryUncertainRecoveryRules.confirmsPolicy(recovery, old, host, policyProof("recovery")));
        for (String section : Arrays.asList("recoveryProof", "cpuBefore", "cpuReceipt", "cpuReadback")) {
            for (Object key : ((Map<?, ?>) policyProof("recovery").state.get(section)).keySet()) {
                MemoryAgentResponse broken = policyProof("recovery");
                ((Map<?, ?>) broken.state.get(section)).remove(key);
                assertFalse(section + "." + key, MemoryUncertainRecoveryRules.confirmsPolicy(recovery, old, host, broken));
            }
        }
        MemoryAgentResponse stale = policyProof("recovery"); stale.sampleTime = 1L;
        assertFalse(MemoryUncertainRecoveryRules.confirmsPolicy(recovery, old, host, stale));
        old.setPolicy("{\"writeback\":{\"batchBytes\":8192}}");
        assertFalse(MemoryUncertainRecoveryRules.confirmsPolicy(recovery, old, host, policyProof("recovery")));
    }

    private void rejects(APIUpdateMemoryPolicyMsg msg) {
        try { MemoryUncertainRecoveryRules.validate(msg); fail("invalid recovery was admitted"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_UNCERTAIN_RECOVERY_INVALID", expected.getCode()); }
    }

    @Test public void proofMustMatchEveryHistoricalAndNativeSafetyFact() {
        MemoryTaskVO recovery = new MemoryTaskVO(); recovery.setUuid("recovery"); recovery.setHostUuid("host1");
        recovery.setAction("recoverUncertain"); recovery.setExpectedControlOperationUuid(OLD);
        recovery.setPolicy(org.zstack.utils.gson.JSONObjectUtil.toJsonString(request()));
        MemoryTaskVO old = new MemoryTaskVO(); old.setUuid(OLD); old.setHostUuid("host1");
        old.setScope("Host"); old.setAction("apply"); old.setStatus("Unknown");
        MemoryTaskVO drain = new MemoryTaskVO(); drain.setUuid(DRAIN); drain.setHostUuid("host1");
        drain.setAction("drain"); drain.setStatus("Succeeded");
        MemoryStateVO host = new MemoryStateVO(); host.setHostUuid("host1"); host.setControlOperationUuid(OLD);
        host.setState("{\"bootId\":\"" + BOOT + "\"}");
        assertTrue(MemoryUncertainRecoveryRules.confirms(recovery, old, drain, host, proof("recovery")));
        for (String section : Arrays.asList("recoveryProof", "maintenanceProof")) {
            Map<?, ?> fields = (Map<?, ?>) proof("recovery").state.get(section);
            for (Object field : fields.keySet()) {
                MemoryAgentResponse broken = proof("recovery");
                ((Map<?, ?>) broken.state.get(section)).remove(field);
                assertFalse(section + "." + field,
                        MemoryUncertainRecoveryRules.confirms(recovery, old, drain, host, broken));
            }
        }
        MemoryAgentResponse broken = proof("recovery"); broken.appliedRevision = 1L;
        assertFalse(MemoryUncertainRecoveryRules.confirms(recovery, old, drain, host, broken));
        assertFalse(MemoryUncertainRecoveryRules.confirms(recovery, old, drain, host, proof("other")));
        old.setStatus("Succeeded");
        assertFalse(MemoryUncertainRecoveryRules.confirms(recovery, old, drain, host, proof("recovery")));
    }
}
