package org.zstack.kvm.memory;

import java.util.List;
import java.util.Map;

/** Presentation metrics only. Control and identity still belong to the Host Agent. */
public interface MemoryMetricsExtensionPoint {
    Map<String, Map<String, Object>> hostSavings(List<String> hostUuids, long now, long ttlMillis);
    Map<String, Object> vmAccounting(String hostUuid, List<String> vmUuids, long now, long ttlMillis);
}
