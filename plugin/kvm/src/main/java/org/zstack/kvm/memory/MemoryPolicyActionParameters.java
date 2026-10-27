package org.zstack.kvm.memory;

import java.util.Arrays;

/** Reject action-inapplicable parameters before hashing or persistence. */
final class MemoryPolicyActionParameters {
    private MemoryPolicyActionParameters() { }

    static void validate(MemoryActionRequest msg) {
        String action = msg.getAction();
        if (!Arrays.asList("pause", "drain", "resume", "reconcile",
                "recoverUncertain", "prepareWritebackBackend").contains(action)) {
            invalid("action");
        }
    }

    private static void invalid(String field) {
        throw new MemoryOperationException("MEMORY_INVALID_REQUEST", "Parameter is not applicable to this action: " + field);
    }
}
