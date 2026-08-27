package org.zstack.header.physicalserver;

import org.zstack.header.core.Completion;

import java.util.Collection;
import java.util.Collections;
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

    void ensureResourceAssignments(Collection<String> serverUuids, String roleType);

    default void ensureResourceAssignment(String serverUuid, String roleType) {
        ensureResourceAssignments(Collections.singleton(serverUuid), roleType);
    }

    void reconcile(String serverUuid);

    void refreshAndReconcile(String serverUuid);

    void reconcileAll();

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
