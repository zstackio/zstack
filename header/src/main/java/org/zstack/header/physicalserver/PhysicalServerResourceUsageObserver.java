package org.zstack.header.physicalserver;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;

public interface PhysicalServerResourceUsageObserver {
    PhysicalServerRoleType getRoleType();

    void collectManagedServiceUsage(
            String serverUuid, ResourceControlCommand command,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion);
}
