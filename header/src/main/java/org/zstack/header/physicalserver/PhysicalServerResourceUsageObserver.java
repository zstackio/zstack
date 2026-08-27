package org.zstack.header.physicalserver;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PhysicalServerResourceUsageObserver {
    /**
     * @return the stable Role type whose service usage this observer reports
     */
    String getRoleType();

    /**
     * Refreshes all PhysicalServer associations owned by this observer.
     */
    default void refreshAssociations() {
    }

    /**
     * Refreshes associations for the requested PhysicalServers.
     *
     * @param serverUuids PhysicalServer UUIDs to refresh; {@code null} or empty
     *                    requests a full refresh
     */
    default void refreshAssociations(Collection<String> serverUuids) {
        refreshAssociations();
    }

    /**
     * @return a non-null snapshot of associated PhysicalServer UUIDs
     */
    Set<String> getAssociatedServerUuids();

    /**
     * Collects current service resource usage for one PhysicalServer.
     * Implementations must complete the callback exactly once, using success
     * for a non-null usage list or failure when the request cannot be completed.
     *
     * @param serverUuid PhysicalServer UUID to inspect
     * @param includeAuxiliaryServices whether auxiliary services are included
     * @param completion callback for the collected usage or collection failure
     */
    void collectManagedServiceUsage(
            String serverUuid,
            boolean includeAuxiliaryServices,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion);
}
