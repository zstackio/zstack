package org.zstack.kvm.memory;

/** A transport ACK is not an application result. Fence by operation and revision. */
public final class MemoryResultRules {
    private MemoryResultRules() { }
    public static String status(String operation, long revision, MemoryAgentResponse response) {
        if (response == null || !operation.equals(response.operationUuid)) { return "Unknown"; }
        if ("Blocked".equals(response.status)) {
            return knownBlocked(response) ? "Blocked" : "Unknown";
        }
        if ("Failed".equals(response.status) && !response.isSuccess()) { return "Failed"; }
        if (!response.isSuccess()) { return "Unknown"; }
        if ("Draining".equals(response.status)) { return "Draining"; }
        if (!"Succeeded".equals(response.status)) { return "Unknown"; }
        return revision < 0 || (response.appliedRevision != null && response.appliedRevision == revision)
                ? "Succeeded" : "Unknown";
    }

    private static boolean knownBlocked(MemoryAgentResponse response) {
        if (response.isSuccess() || response.appliedRevision != null || response.state == null
                || !Boolean.TRUE.equals(response.state.get("knownBlocked"))
                || !"BLOCKED".equals(response.state.get("phase"))) { return false; }
        Object blockers = response.state.get("blockers");
        if (!(blockers instanceof java.util.List) || ((java.util.List<?>) blockers).isEmpty()) { return false; }
        for (Object item : (java.util.List<?>) blockers) {
            if (!(item instanceof java.util.Map)) { return false; }
            java.util.Map<?, ?> blocker = (java.util.Map<?, ?>) item;
            // This is the only native, receipt-backed blocker in the current ABI.
            // Arbitrary stderr or a transport failure is not a known application state.
            if (!"capacity".equals(blocker.get("section"))
                    || !"NATIVE_POOL_UNQUALIFIED".equals(blocker.get("code"))) { return false; }
        }
        return true;
    }
}
