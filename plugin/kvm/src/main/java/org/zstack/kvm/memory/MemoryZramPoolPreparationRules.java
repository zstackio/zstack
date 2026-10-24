package org.zstack.kvm.memory;

/** Admission contract for the internal reset of an already-drained RAM-only ZRAM pool. */
public final class MemoryZramPoolPreparationRules {
    public static final String ACTION = "prepareZramPool";
    private MemoryZramPoolPreparationRules() { }

    public static void validate(MemoryZramPoolPreparation request, String drainOperationUuid) {
        if (request == null || !request.isResetConfirmed()
                || request.getExpectedHostBootId() == null
                || !request.getExpectedHostBootId().matches(
                        "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
                || request.getExpectedPoolGeneration() == null
                || !request.getExpectedPoolGeneration().matches("[a-f0-9]{64}")
                || drainOperationUuid == null || !drainOperationUuid.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new MemoryOperationException("MEMORY_ZRAM_POOL_PREPARATION_INVALID",
                    "Pool preparation requires the current boot, drained pool generation, drain control operation and reset confirmation");
        }
    }
}
