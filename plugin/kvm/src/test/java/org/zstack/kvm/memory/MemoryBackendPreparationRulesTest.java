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

    @Test public void acceptsAnExplicitHostBoundPreparation() {
        MemoryBackendPreparationRules.validate(preparation());
    }

    @Test public void rejectsMissingConfirmationUnsafeNumbersAndUnboundIdentity() {
        reject(null);
        MemoryBackendPreparation value = preparation(); value.setResetConfirmed(false); reject(value);
        value = preparation(); value.setBackendCapacityBytes(4096); reject(value);
        value = preparation(); value.setBackendCapacityBytes((20L << 30) + 1); reject(value);
        value = preparation(); value.setExpectedHostBootId(""); reject(value);
        value = preparation(); value.setExpectedPoolGeneration("unknown"); reject(value);
        value = preparation(); value.setCandidateId("/dev/vdb"); reject(value);
    }

    private void reject(MemoryBackendPreparation value) {
        try { MemoryBackendPreparationRules.validate(value); fail("unsafe request was accepted"); }
        catch (IllegalArgumentException expected) { }
    }
}
