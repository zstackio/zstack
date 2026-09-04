package org.zstack.header.physicalserver;

import org.zstack.header.core.Completion;

import java.util.Collection;
import java.util.Map;

public interface PhysicalServerManager {
    /**
     * Resolves machine identities to PhysicalServers.
     *
     * @return normalized serial number to server UUID. An unresolved serial
     * number is absent from the returned map.
     */
    Map<String, String> resolveBySerialNumbers(Collection<String> serialNumbers);

    void associationChanged(String serverUuid);

    void releaseResourceAssignment(String serverUuid, String roleType, Completion completion);

    void forceReleaseResourceAssignment(String serverUuid, String roleType, Completion completion);
}
