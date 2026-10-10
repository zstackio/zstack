package org.zstack.kvm.memory;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryBackendPreparationRulesTest {
    static MemoryBackendPreparation preparation() {
        MemoryBackendPreparation value = new MemoryBackendPreparation();
        value.setCandidateId("candidate:" + String.join("", java.util.Collections.nCopies(64, "a")));
        value.setExpectedHostBootId("1e73a145-2662-4911-9c4f-b75a7d91a88e");
        value.setExpectedPoolGeneration("none");
        value.setBackendCapacityBytes(20L << 30);
        value.setResetConfirmed(true);
        return value;
    }

    private APIUpdateMemoryPolicyMsg request() {
        APIUpdateMemoryPolicyMsg message = new APIUpdateMemoryPolicyMsg();
        message.setScope("Host"); message.setAction("prepareWritebackBackend");
        message.setBackendPreparation(preparation()); return message;
    }

    @Test public void acceptsExplicitHostBoundPreparationWithoutChangingPolicy() {
        APIUpdateMemoryPolicyMsg message = request();
        MemoryBackendPreparationRules.validate(message);
        assertEquals("{}", message.getPolicy());
        assertTrue(MemoryTaskRules.requiresLicenseForConfiguration(message.getAction(), message.getPolicy()));
    }

    @Test public void rejectsBulkScopePathsMissingConfirmationAndUnsafeNumbers() {
        for (String scope : new String[]{"Global", "Cluster", "VM"}) {
            APIUpdateMemoryPolicyMsg message = request(); message.setScope(scope); reject(message);
        }
        APIUpdateMemoryPolicyMsg message = request(); message.setBackendPreparation(null); reject(message);
        message = request(); message.getBackendPreparation().setResetConfirmed(false); reject(message);
        message = request(); message.getBackendPreparation().setBackendCapacityBytes(4096); reject(message);
        message = request(); message.setPolicy("{\"zram\":{\"enabled\":true}}"); reject(message);
    }

    @Test public void rejectsUnboundIdentityAndSmuggledPreparationOnOtherActions() {
        APIUpdateMemoryPolicyMsg message = request(); message.getBackendPreparation().setExpectedHostBootId(""); reject(message);
        message = request(); message.getBackendPreparation().setExpectedPoolGeneration("unknown"); reject(message);
        message = request(); message.getBackendPreparation().setCandidateId("/dev/vdb"); reject(message);
        message = request(); message.setAction("apply"); reject(message);
        message = request(); message.setBackendPreparation(null); reject(message);
        message = request(); message.setTargetHostUuids(java.util.Collections.singletonList("host")); reject(message);
    }

    private void reject(APIUpdateMemoryPolicyMsg message) {
        try { MemoryBackendPreparationRules.validate(message); fail("unsafe request was accepted"); }
        catch (IllegalArgumentException expected) { }
    }
}
