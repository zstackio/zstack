package org.zstack.kvm.memory;

/** Pure coordination rules kept separate so fan-out safety is unit-testable. */
final class MemoryVmAccountingBatchRules {
    static final int MAX_IN_FLIGHT_HOSTS = 4;
    static final long DEADLINE_SECONDS = 60;

    private MemoryVmAccountingBatchRules() {
    }

    static int width(int requested) {
        return Math.max(1, Math.min(MAX_IN_FLIGHT_HOSTS, requested));
    }

    static String unavailableReason(boolean hostResponseSuccessful, boolean placementChanged) {
        if (placementChanged) {
            return "MEMORY_INSTANCE_CHANGED";
        }
        return hostResponseSuccessful ? null : "HOST_ACCOUNTING_UNAVAILABLE";
    }
}
