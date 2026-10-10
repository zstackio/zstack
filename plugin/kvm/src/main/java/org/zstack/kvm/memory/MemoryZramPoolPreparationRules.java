package org.zstack.kvm.memory;

import com.google.gson.JsonParser;

/** Admission contract for explicit reset of an already-drained RAM-only ZRAM pool. */
public final class MemoryZramPoolPreparationRules {
    public static final String ACTION = "prepareZramPool";
    private MemoryZramPoolPreparationRules() { }

    public static void validate(APIUpdateMemoryPolicyMsg message) {
        MemoryZramPoolPreparation request = message.getPoolPreparation();
        if (!ACTION.equals(message.getAction())) {
            if (request != null) { invalid(); }
            return;
        }
        if (!"Host".equals(message.getScope()) || message.getTargetHostUuids() != null ||
                request == null || !request.isResetConfirmed()) {
            invalid();
        }
        try {
            if (message.getPolicy() == null ||
                    new JsonParser().parse(message.getPolicy()).getAsJsonObject().size() != 0) { invalid(); }
            if (request.getExpectedHostBootId() == null ||
                    !request.getExpectedHostBootId().matches(
                            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")) {
                invalid();
            }
            if (request.getExpectedPoolGeneration() == null ||
                    !request.getExpectedPoolGeneration().matches("[a-f0-9]{64}")) { invalid(); }
            if (!(message.getExpectedControlOperationUuid() instanceof String) ||
                    !((String) message.getExpectedControlOperationUuid()).matches("[A-Za-z0-9_-]{1,32}")) { invalid(); }
        } catch (RuntimeException error) { invalid(); }
    }

    private static void invalid() {
        throw new MemoryOperationException("MEMORY_ZRAM_POOL_PREPARATION_INVALID",
                "Explicit Host pool preparation requires the current boot, drained pool generation, control operation UUID and reset confirmation");
    }
}
