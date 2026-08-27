package org.zstack.header.physicalserver;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PhysicalServerResourceUsageObserver {
    String getRoleType();

    default void refreshAssociations() {
    }

    default void refreshAssociations(Collection<String> serverUuids) {
        refreshAssociations();
    }

    Set<String> getAssociatedServerUuids();

    void collectManagedServiceUsage(
            String serverUuid,
            boolean includeAuxiliaryServices,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion);
}
