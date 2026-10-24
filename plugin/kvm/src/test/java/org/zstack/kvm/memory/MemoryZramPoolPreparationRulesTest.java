package org.zstack.kvm.memory;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryZramPoolPreparationRulesTest {
    private static final String BOOT = "123e4567-e89b-12d3-a456-426614174000";
    private static final String DRAIN = "093d46206e694835b4a218449eb1bc7c";

    static MemoryZramPoolPreparation preparation() {
        MemoryZramPoolPreparation request = new MemoryZramPoolPreparation();
        request.setExpectedHostBootId(BOOT);
        request.setExpectedPoolGeneration(new String(new char[64]).replace('\0', 'a'));
        request.setResetConfirmed(true);
        return request;
    }

    @Test public void acceptsOnlyPoolResetBoundToDrain() {
        MemoryZramPoolPreparationRules.validate(preparation(), DRAIN);
    }

    @Test public void rejectsFirstUseOrUnconfirmedReset() {
        MemoryZramPoolPreparation request = preparation(); request.setExpectedPoolGeneration("none");
        assertInvalid(request, DRAIN);
        request = preparation(); request.setResetConfirmed(false);
        assertInvalid(request, DRAIN);
    }

    @Test public void rejectsMissingOrMalformedDrainFence() {
        assertInvalid(preparation(), null);
        MemoryZramPoolPreparation request = preparation(); request.setExpectedHostBootId("stale");
        assertInvalid(request, DRAIN);
        request = preparation(); request.setExpectedPoolGeneration("ab");
        assertInvalid(request, DRAIN);
    }

    private static void assertInvalid(MemoryZramPoolPreparation request, String drain) {
        try { MemoryZramPoolPreparationRules.validate(request, drain); fail("unsafe request accepted"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_ZRAM_POOL_PREPARATION_INVALID", expected.getCode()); }
    }
}
