package org.zstack.kvm.memory;

import java.util.List;
import java.util.Map;

/** Presentation metrics only. Control and identity still belong to the Host Agent. */
public interface MemoryMetricsExtensionPoint {
    /** Throws when no monitoring node answers; a partial answer keeps its samples and names its reason. */
    MemoryHostSavingsSamples hostSavings(List<String> hostUuids, long now, long ttlMillis);
    Map<String, Object> vmAccounting(String hostUuid, List<String> vmUuids, long now, long ttlMillis);
}
