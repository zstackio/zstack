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
    Map<String, String> resolveIdentities(Collection<PhysicalServerIdentitySpec> identities);

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

    void reconcile(String serverUuid, boolean refreshFacts);

    void reconcileAll();

    default void releaseResourceAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            Completion completion) {
        releaseResourceAssignment(
                serverUuid, roleType, consumerUuid, false, completion);
    }

    /**
     * Releases a role assignment.
     *
     * @param force when true, forgets the assignment if the external
     *              constraint cannot be released; the release failure is
     *              still returned to the caller
     */
    void releaseResourceAssignment(
            String serverUuid,
            String roleType,
            String consumerUuid,
            boolean force,
            Completion completion);
}
