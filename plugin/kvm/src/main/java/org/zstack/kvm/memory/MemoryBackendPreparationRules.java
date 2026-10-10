package org.zstack.kvm.memory;

import com.google.gson.JsonParser;

/** The public maintenance request identifies a discovered device, never a path. */
public final class MemoryBackendPreparationRules {
    public static final String ACTION = "prepareWritebackBackend";
    private MemoryBackendPreparationRules() { }

    public static void validate(APIUpdateMemoryPolicyMsg message) {
        MemoryBackendPreparation request = message.getBackendPreparation();
        if (!ACTION.equals(message.getAction())) {
            if (request != null) { invalid(); }
            return;
        }
        if (!"Host".equals(message.getScope()) || message.getTargetHostUuids() != null
                || request == null || !request.isResetConfirmed()) { invalid(); }
        try {
            if (message.getPolicy() == null || new JsonParser().parse(message.getPolicy()).getAsJsonObject().size() != 0) { invalid(); }
            if (request.getCandidateId() == null || !request.getCandidateId().matches("candidate:[a-f0-9]{64}")) { invalid(); }
            if (request.getExpectedHostBootId() == null || !request.getExpectedHostBootId().matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")) { invalid(); }
            if (request.getExpectedPoolGeneration() == null || !request.getExpectedPoolGeneration().matches("none|[a-f0-9]{64}")) { invalid(); }
            long bytes = request.getBackendCapacityBytes();
            // The GraphQL/JSON UI path must carry the exact integer, not a rounded IEEE double.
            if (bytes < 8192 || bytes > 9007199254740991L || bytes % 4096 != 0) { invalid(); }
        } catch (RuntimeException error) { invalid(); }
    }

    private static void invalid() {
        throw new MemoryOperationException("MEMORY_BACKEND_PREPARATION_INVALID",
                "Explicit Host maintenance requires a bound candidate, boot and pool identity, exact capacity and reset confirmation");
    }
}
