package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryUncertainRecoveryRulesTest {
    static final String OLD = "11111111111111111111111111111111";
    static final String DRAIN = "22222222222222222222222222222222";
    static final String ANCHOR = "44444444444444444444444444444444";
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
        receipt.put("_http_status", 200);
        response.state.put("cpuReceipt", receipt);
        Map<String, Object> readback = new LinkedHashMap<>(receipt); readback.remove("request_id");
        response.state.put("cpuReadback", readback);
        return response;
    }

    static MemoryAgentResponse partialServicePolicyProof(String operation, String originalPolicy) {
        MemoryAgentResponse response = policyProof(operation);
        Map<String, Object> proof = (Map<String, Object>) response.state.get("recoveryProof");
        proof.put("kind", "partialServiceApply");
        proof.put("cpuExpectedRevision", 28);
        Map<String, Object> cpuBefore = (Map<String, Object>) response.state.get("cpuBefore");
        cpuBefore.put("revision", 27);
        ((Map<String, Object>) response.state.get("cpuReceipt")).put("revision", 29);
        ((Map<String, Object>) response.state.get("cpuReceipt")).put("_http_status", 200);
        ((Map<String, Object>) response.state.get("cpuReadback")).put("revision", 29);
        ((Map<String, Object>) response.state.get("cpuReadback")).put("_http_status", 200);
        proof.put("originalPolicyHash", org.apache.commons.codec.digest.DigestUtils.sha256Hex(
                MemoryPolicyRules.canonicalRequest(MemoryPolicyRules.decode(originalPolicy))));
        proof.put("originalJournalHash", String.join("", Collections.nCopies(64, "d")));
        proof.put("anchorOperationUuid", ANCHOR);
        proof.put("anchorJournalHash", String.join("", Collections.nCopies(64, "e")));
        proof.put("cpuExpectedRevision", 28);
        proof.put("tuningExpectedRevision", 13);
        proof.put("tuningRevision", 14);
        proof.put("tuningRequestId", OLD + "-tuning");
        Map<String, Object> originalKsmReadback = new LinkedHashMap<>();
        originalKsmReadback.put("configRevision", "9");
        originalKsmReadback.put("run", "1");
        originalKsmReadback.put("use_zero_pages", "1");
        originalKsmReadback.put("pages_to_scan", "2000");
        originalKsmReadback.put("sleep_millisecs", "20");
        proof.put("originalKsmReadback", originalKsmReadback);
        Map<String, Object> cpuGuard = new LinkedHashMap<>();
        cpuGuard.put("revision", 28); cpuGuard.put("efficiency_enabled", false);
        cpuGuard.put("idle_cpu_threshold_percent", 5); cpuGuard.put("idle_duration_seconds", 60);
        cpuGuard.put("_http_status", 200); cpuGuard.put("request_id", OLD + "-cpu-hold");
        proof.put("cpuGuard", cpuGuard);
        cpuGuard.put("_http_status", 200);
        proof.put("cpuGuardReadback", new LinkedHashMap<>(cpuGuard));
        Map<String, Object> beforeSettings = new TreeMap<>();
        beforeSettings.put("algorithm", "lzo");
        beforeSettings.put("host_cpu_guard_enabled", true);
        Map<String, Object> beforeWriteback = new TreeMap<>();
        beforeWriteback.put("enabled", true);
        beforeWriteback.put("batch_bytes", 67108864L);
        beforeSettings.put("writeback", beforeWriteback);
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("_http_status", 200); before.put("revision", 13);
        before.put("settings", beforeSettings);
        proof.put("tuningBefore", before);

        Map<String, Object> targetSettings = new TreeMap<>(beforeSettings);
        Map<String, Object> targetWriteback = new TreeMap<>(beforeWriteback);
        targetWriteback.put("enabled", false);
        targetSettings.put("writeback", targetWriteback);
        Map<String, Object> command = new TreeMap<>();
        command.put("request_id", OLD + "-tuning");
        command.put("expected_revision", 13);
        Map<String, Object> commandSettings = new TreeMap<>(targetSettings);
        commandSettings.put("writeback", new TreeMap<>(targetWriteback));
        command.put("settings", commandSettings);
        proof.put("tuningCommand", command);
        proof.put("tuningPayloadSha256", org.apache.commons.codec.digest.DigestUtils.sha256Hex(
                MemoryPolicyRules.canonicalRequest(command)));

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("_http_status", 200); receipt.put("revision", 14);
        Map<String, Object> receiptSettings = new TreeMap<>(targetSettings);
        receiptSettings.put("writeback", new TreeMap<>(targetWriteback));
        receipt.put("settings", receiptSettings); receipt.put("agent_request_id", OLD + "-tuning");
        receipt.put("agent_expected_revision", 13);
        proof.put("tuningReceipt", receipt);
        Map<String, Object> readback = new LinkedHashMap<>();
        readback.put("_http_status", 200); readback.put("revision", 14);
        Map<String, Object> readbackSettings = new TreeMap<>(targetSettings);
        readbackSettings.put("writeback", new TreeMap<>(targetWriteback));
        readback.put("settings", readbackSettings);
        proof.put("tuningReadback", readback);
        return response;
    }

    private MemoryActionRequest message() {
        MemoryActionRequest msg = new MemoryActionRequest();
        msg.setScope("Host"); msg.setResourceUuid("host1"); msg.setAction("recoverUncertain");
        msg.setExpectedControlOperationUuid(OLD); msg.setRecovery(requestDto()); return msg;
    }

    @Test public void requiresExplicitNarrowHostRecoveryWithoutPolicyWrites() {
        MemoryUncertainRecoveryRules.validate(message());
        MemoryActionRequest ordinary = message();
        ordinary.getRecovery().setDrainControlOperationUuid(null);
        MemoryUncertainRecoveryRules.validate(ordinary);
        MemoryActionRequest msg = message(); msg.getRecovery().setConfirmed(false); rejects(msg);
        msg = message(); msg.setAction("apply"); rejects(msg);
        msg = message(); msg.setScope("Global"); rejects(msg);
        msg = message(); msg.setTargetHostUuids(Collections.singletonList("host1")); rejects(msg);
        msg = message(); msg.setPolicy("{\"zram\":{\"enabled\":true}}"); rejects(msg);
        msg = message(); msg.getRecovery().setExpectedPoolGeneration("../path"); rejects(msg);
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

    @Test public void partialServiceRecoveryRequiresAnchorBoundSameRequestTuningReceipt() {
        MemoryTaskVO recovery = new MemoryTaskVO(); recovery.setUuid("recovery"); recovery.setHostUuid("host1");
        recovery.setScope("Host"); recovery.setAction("recoverUncertain"); recovery.setDesiredRevision(9);
        recovery.setExpectedControlOperationUuid(OLD);
        Map<String, Object> request = request(); request.remove("drainControlOperationUuid");
        recovery.setPolicy(org.zstack.utils.gson.JSONObjectUtil.toJsonString(request));
        MemoryTaskVO old = new MemoryTaskVO(); old.setUuid(OLD); old.setHostUuid("host1"); old.setScope("Host");
        old.setAction("apply"); old.setStatus("Unknown"); old.setDesiredRevision(9);
        old.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true},\"writeback\":{\"enabled\":false}}");
        MemoryStateVO host = new MemoryStateVO(); host.setHostUuid("host1"); host.setControlOperationUuid(OLD);
        host.setDesiredRevision(9); host.setAppliedRevision(8L); host.setState("{\"bootId\":\"" + BOOT + "\"}");

        String originalPolicy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true},\"writeback\":{\"enabled\":false}}";
        old.setPolicy(originalPolicy);
        MemoryAgentResponse valid = partialServicePolicyProof("recovery", originalPolicy);
        MemoryTaskVO anchor = new MemoryTaskVO();
        anchor.setUuid(ANCHOR); anchor.setHostUuid("host1"); anchor.setResourceUuid("host1");
        anchor.setScope("Host"); anchor.setAction("apply"); anchor.setStatus("Succeeded");
        anchor.setDesiredRevision(8);
        anchor.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":false},"
                + "\"writeback\":{\"enabled\":true,\"backendResourceUuid\":\"33333333333333333333333333333333\","
                + "\"backendCapacityBytes\":21474836480}}");
        valid.hostUuid = "host1";
        valid = org.zstack.utils.gson.JSONObjectUtil.toObject(
                org.zstack.utils.gson.JSONObjectUtil.toJsonString(valid), MemoryAgentResponse.class);
        assertTrue(MemoryUncertainRecoveryRules.confirmsPolicy(recovery, old, anchor, host, valid));
        MemoryAgentResponse omittedOuterHost = MemoryUncertainRecoveryRulesTest.partialServicePolicyProof(
                "recovery", originalPolicy);
        omittedOuterHost = org.zstack.utils.gson.JSONObjectUtil.toObject(
                org.zstack.utils.gson.JSONObjectUtil.toJsonString(omittedOuterHost), MemoryAgentResponse.class);
        assertTrue("the optional outer host identity remains backwards compatible",
                MemoryUncertainRecoveryRules.confirmsPolicy(recovery, old, anchor, host, omittedOuterHost));
        for (String invalid : Arrays.asList("missingKind", "missingTuningReadback", "missingKsmReadback",
                "missingCpuGuard", "cpuGuardRevision", "ksmRevision", "ksmRun", "ksmZeroPages", "anchorOperation", "anchorJournal", "tuningRequestId",
                "tuningExpectedRevision", "tuningPayloadHash", "commandRequestId", "commandRevision",
                "otherSettingChanged", "receiptStatus", "receiptRequestId", "receiptExpectedRevision",
                "receiptRevision", "readbackRevision", "readbackSettings", "guardRequestId",
                "finalCpuStatus", "notFinalCpu", "anchorRevision", "numericKsmRevision", "foreignResponseHost")) {
            MemoryAgentResponse broken = partialServicePolicyProof("recovery", originalPolicy);
            if ("foreignResponseHost".equals(invalid)) broken.hostUuid = "host2";
            Map<String, Object> proof = (Map<String, Object>) broken.state.get("recoveryProof");
            if ("missingKind".equals(invalid)) proof.remove("kind");
            if ("missingTuningReadback".equals(invalid)) proof.remove("tuningReadback");
            if ("missingKsmReadback".equals(invalid)) proof.remove("originalKsmReadback");
            if ("missingCpuGuard".equals(invalid)) proof.remove("cpuGuardReadback");
            if ("cpuGuardRevision".equals(invalid)) ((Map<String, Object>) proof.get("cpuGuardReadback")).put("revision", 12);
            Map<String, Object> ksmReadback = (Map<String, Object>) proof.get("originalKsmReadback");
            if ("ksmRevision".equals(invalid)) ksmReadback.put("configRevision", 8);
            if ("ksmRun".equals(invalid)) ksmReadback.put("run", "0");
            if ("ksmZeroPages".equals(invalid)) ksmReadback.put("use_zero_pages", "0");
            if ("anchorOperation".equals(invalid)) proof.put("anchorOperationUuid", OLD);
            if ("anchorJournal".equals(invalid)) proof.put("anchorJournalHash", "bad");
            if ("tuningRequestId".equals(invalid)) proof.put("tuningRequestId", OLD + "-other");
            if ("tuningExpectedRevision".equals(invalid)) proof.put("tuningExpectedRevision", 12);
            if ("tuningPayloadHash".equals(invalid)) proof.put("tuningPayloadSha256", "bad");
            Map<String, Object> command = (Map<String, Object>) proof.get("tuningCommand");
            Map<String, Object> receipt = (Map<String, Object>) proof.get("tuningReceipt");
            Map<String, Object> readback = (Map<String, Object>) proof.get("tuningReadback");
            if ("commandRequestId".equals(invalid)) command.put("request_id", OLD + "-other");
            if ("commandRevision".equals(invalid)) command.put("expected_revision", 12);
            if ("otherSettingChanged".equals(invalid)) {
                ((Map<String, Object>) command.get("settings")).put("host_cpu_guard_enabled", false);
            }
            if ("receiptStatus".equals(invalid)) receipt.put("_http_status", 409);
            if ("receiptRequestId".equals(invalid)) receipt.put("agent_request_id", OLD + "-other");
            if ("receiptExpectedRevision".equals(invalid)) receipt.put("agent_expected_revision", 12);
            if ("receiptRevision".equals(invalid)) receipt.put("revision", 15);
            if ("finalCpuStatus".equals(invalid)) ((Map<String, Object>) broken.state.get("cpuReceipt")).put("_http_status", 500);
            if ("notFinalCpu".equals(invalid)) proof.put("finalCpuRestored", false);
            if ("guardRequestId".equals(invalid)) ((Map<String, Object>) proof.get("cpuGuard")).put("request_id", OLD + "-other");
            if ("anchorRevision".equals(invalid)) anchor.setDesiredRevision(7);
            if ("numericKsmRevision".equals(invalid)) ksmReadback.put("configRevision", 9);
            if ("readbackRevision".equals(invalid)) readback.put("revision", 15);
            if ("readbackSettings".equals(invalid)) {
                ((Map<String, Object>) readback.get("settings")).put("algorithm", "other");
            }
            assertFalse(invalid, MemoryUncertainRecoveryRules.confirmsPolicy(recovery, old, anchor, host, broken));
            if ("anchorRevision".equals(invalid)) anchor.setDesiredRevision(8);
        }
    }

    private void rejects(MemoryActionRequest msg) {
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
