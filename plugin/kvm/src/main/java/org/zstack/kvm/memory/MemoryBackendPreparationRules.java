package org.zstack.kvm.memory;

import java.util.List;
import java.util.Map;

/** A preparation names a discovered disk, never a path; the Host's current inventory decides whether it can start. */
public final class MemoryBackendPreparationRules {
    public static final String ACTION = "prepareWritebackBackend";
    private MemoryBackendPreparationRules() { }

    public static void validate(String candidateId) {
        if (candidateId == null || !candidateId.matches("candidate:[a-f0-9]{64}")) {
            throw new MemoryOperationException("MEMORY_BACKEND_PREPARATION_INVALID",
                    "A writeback preparation requires a candidate id from GetHostMemoryWritebackBackends");
        }
    }

    /** The candidate of the Agent inventory with this id, or null. */
    static Map<?, ?> candidate(Map<String, Object> inventory, String candidateId) {
        Object candidates = inventory.get("candidates");
        if (!(candidates instanceof List)) { return null; }
        for (Object candidate : (List<?>) candidates) {
            if (candidate instanceof Map && candidateId.equals(((Map<?, ?>) candidate).get("identity"))) {
                return (Map<?, ?>) candidate;
            }
        }
        return null;
    }

    /** Why the disk cannot be prepared on the Host now, or null when it can. */
    static String refusal(Map<String, Object> inventory, String candidateId) {
        Map<?, ?> candidate = candidate(inventory, candidateId);
        if (candidate == null) { return "CANDIDATE_NOT_FOUND"; }
        if (!"NEEDS_OFFLINE_PREPARATION".equals(candidate.get("state"))) { return "CANDIDATE_NOT_AVAILABLE"; }
        Object reasons = inventory.get("preparationReasons");
        if (!(reasons instanceof List)) { return "PREPARATION_STATE_UNKNOWN"; }
        if (!((List<?>) reasons).isEmpty()) { return String.valueOf(((List<?>) reasons).get(0)); }
        if (!(inventory.get("bootId") instanceof String)
                || !String.valueOf(inventory.get("poolGeneration")).matches("none|[a-f0-9]{64}")) {
            return "HOST_IDENTITY_UNKNOWN";
        }
        Object capacity = candidate.get("capacityBytes");
        long bytes = capacity instanceof Number ? ((Number) capacity).longValue() : -1;
        // Only an exact whole number of 4 KiB pages can back ZRAM writeback.
        if (!(capacity instanceof Number) || bytes != ((Number) capacity).doubleValue()
                || bytes < 8192 || bytes > 9007199254740991L || bytes % 4096 != 0) {
            return "CANDIDATE_CAPACITY_INVALID";
        }
        return null;
    }
}
