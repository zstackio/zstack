package org.zstack.kvm.memory;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryZramPoolPreparationRulesTest {
    private static final String BOOT = "123e4567-e89b-12d3-a456-426614174000";
    private static final String CONTROL = "093d46206e694835b4a218449eb1bc7c";

    static MemoryZramPoolPreparation preparation() {
        MemoryZramPoolPreparation request = new MemoryZramPoolPreparation();
        request.setExpectedHostBootId(BOOT);
        request.setExpectedPoolGeneration(new String(new char[64]).replace('\0', 'a'));
        request.setResetConfirmed(true);
        return request;
    }

    private APIUpdateMemoryPolicyMsg valid() {
        APIUpdateMemoryPolicyMsg message = new APIUpdateMemoryPolicyMsg();
        message.setAction(MemoryZramPoolPreparationRules.ACTION);
        message.setScope("Host"); message.setResourceUuid("093d46206e694835b4a218449eb1bc7c");
        message.setPolicy("{}"); message.setExpectedControlOperationUuid(CONTROL);
        message.setPoolPreparation(preparation());
        return message;
    }

    @Test public void acceptsOnlyExplicitHostPoolResetBoundToDrain() {
        MemoryZramPoolPreparationRules.validate(valid());
    }

    @Test public void rejectsFirstUseOrUnconfirmedReset() {
        APIUpdateMemoryPolicyMsg message = valid();
        message.getPoolPreparation().setExpectedPoolGeneration("none");
        assertInvalid(message);
        message = valid(); message.getPoolPreparation().setResetConfirmed(false);
        assertInvalid(message);
    }

    @Test public void rejectsNonHostPolicyTargetsAndUnexpectedFields() {
        APIUpdateMemoryPolicyMsg message = valid(); message.setScope("Cluster"); assertInvalid(message);
        message = valid(); message.setPolicy("{\"zram\":{\"enabled\":true}}"); assertInvalid(message);
        message = valid(); message.setTargetHostUuids(java.util.Collections.singletonList("host")); assertInvalid(message);
    }

    @Test public void rejectsMissingOrMalformedDrainFence() {
        APIUpdateMemoryPolicyMsg message = valid(); message.setExpectedControlOperationUuid(null); assertInvalid(message);
        message = valid(); message.getPoolPreparation().setExpectedHostBootId("stale"); assertInvalid(message);
        message = valid(); message.getPoolPreparation().setExpectedPoolGeneration("ab"); assertInvalid(message);
    }

    private static void assertInvalid(APIUpdateMemoryPolicyMsg message) {
        try { MemoryZramPoolPreparationRules.validate(message); fail("unsafe request accepted"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_ZRAM_POOL_PREPARATION_INVALID", expected.getCode()); }
    }
}
