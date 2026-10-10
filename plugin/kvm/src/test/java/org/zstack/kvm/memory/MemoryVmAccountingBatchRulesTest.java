package org.zstack.kvm.memory;

import org.junit.Test;

import static org.junit.Assert.*;

public class MemoryVmAccountingBatchRulesTest {
    @Test public void fanoutWidthNeverExceedsFourAndNeverDropsToZero() {
        assertEquals(1, MemoryVmAccountingBatchRules.width(0));
        assertEquals(2, MemoryVmAccountingBatchRules.width(2));
        assertEquals(4, MemoryVmAccountingBatchRules.width(4));
        assertEquals(4, MemoryVmAccountingBatchRules.width(100));
        assertEquals(4, MemoryVmAccountingBatchRules.MAX_IN_FLIGHT_HOSTS);
    }

    @Test public void partialHostFailureAndMigrationHaveDistinctReasons() {
        assertEquals("HOST_ACCOUNTING_UNAVAILABLE",
                MemoryVmAccountingBatchRules.unavailableReason(false, false));
        assertEquals("MEMORY_INSTANCE_CHANGED",
                MemoryVmAccountingBatchRules.unavailableReason(true, true));
        assertNull(MemoryVmAccountingBatchRules.unavailableReason(true, false));
        assertEquals(60, MemoryVmAccountingBatchRules.DEADLINE_SECONDS);
    }
}
