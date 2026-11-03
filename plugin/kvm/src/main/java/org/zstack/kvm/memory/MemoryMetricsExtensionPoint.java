package org.zstack.kvm.memory;

import java.util.List;

/** Presentation metrics only. Control and identity still belong to the Host Agent. */
public interface MemoryMetricsExtensionPoint {
    /** Throws when no monitoring node answers; a partial answer keeps its samples and names its reason. */
    MemoryHostSavingsSamples hostSavings(List<String> hostUuids, long now, long ttlMillis);
}
