package org.zstack.kvm.memory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Shared by admission, license gates and the durable task executor. */
public final class MemoryTaskRules {
    private static final Set<String> ACTIVE = new HashSet<>(Arrays.asList(
            "Queued", "Applying", "Draining", "Unknown", "Blocked"));
    private MemoryTaskRules() { }

    public static boolean isSafetyAction(String action, String policy) {
        if (!Arrays.asList("pause", "drain", "resume", "reconcile").contains(action) || policy == null) {
            return false;
        }
        try {
            JsonElement json = new JsonParser().parse(policy);
            return json.isJsonObject() && json.getAsJsonObject().size() == 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Whether a user request must pass the Cloud License configuration gate. */
    public static boolean requiresLicenseForConfiguration(String action, String policy) {
        // Internal recovery tasks may omit the empty policy altogether.  A
        // reconcile with no configuration payload is still read/repair-only.
        return "reconcile".equals(action) ? policy != null && !isSafetyAction(action, policy) : true;
    }

    public static boolean blocksHost(String status) { return ACTIVE.contains(status); }
    /** Only these per-Host states are safe terminal candidates for explicit history cleanup. */
    public static boolean canDeleteHistory(String status) {
        return Arrays.asList("Succeeded", "Failed", "Cancelled").contains(status);
    }
    /** Keep parent aggregation identical for normal result processing and retention checks. */
    public static String aggregateParentStatus(Iterable<String> statuses) {
        java.util.List<String> values = new java.util.ArrayList<>();
        for (String status : statuses) { values.add(status); }
        boolean hasBlocked = values.contains("Blocked");
        return values.contains("Unknown") ? "Unknown"
                : values.stream().anyMatch(s -> !"Blocked".equals(s) && blocksHost(s)) ? "Applying"
                : values.stream().allMatch("Succeeded"::equals) && !hasBlocked ? "Succeeded"
                : values.stream().allMatch("Blocked"::equals) ? "Blocked"
                : values.stream().allMatch("Cancelled"::equals) ? "Cancelled"
                : values.contains("Succeeded") || hasBlocked ? "Partial" : "Failed";
    }
    public static boolean canCancel(String status) { return "Queued".equals(status); }
    public static boolean canTransition(String from, String to) {
        switch (from) {
            case "Queued": return Arrays.asList("Applying", "Cancelled", "Failed").contains(to);
            case "Applying": return Arrays.asList("Draining", "Succeeded", "Failed", "Unknown", "Blocked").contains(to);
            case "Draining": return Arrays.asList("Succeeded", "Failed", "Unknown", "Blocked").contains(to);
            // Reconciliation observes the same operation; it never resubmits it.
            case "Unknown": return Arrays.asList("Succeeded", "Failed", "Blocked").contains(to);
            case "Blocked": return Arrays.asList("Succeeded", "Failed", "Unknown").contains(to);
            default: return false;
        }
    }
}
