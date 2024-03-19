package org.zstack.header.allocator;

import java.util.List;
import java.util.Map;

/**
 */
public interface HostReservedCapacityExtensionPoint {
    String getHypervisorTypeForHostReserveCapacityExtension();

    ReservedHostCapacity getReservedHostCapacity(String hostUuid);

    Map<String, ReservedHostCapacity> getReservedHostsCapacity(List<String> hostUuids);
}
