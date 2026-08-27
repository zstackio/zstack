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

    /**
     * Finds machine identities for PhysicalServers.
     *
     * @return server UUID to normalized serial number. An unresolved server
     * UUID is absent from the returned map.
     */
    Map<String, String> findSerialNumbersByServerUuids(
            Collection<String> serverUuids);

    void associationChanged(String serverUuid);

    void releaseResourceAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion);

    void forceReleaseResourceAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion);
}
