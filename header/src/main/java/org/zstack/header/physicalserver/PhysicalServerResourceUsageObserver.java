package org.zstack.header.physicalserver;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;

public interface PhysicalServerResourceUsageObserver {
    String getRoleType();

    void collectManagedServiceUsage(
            String serverUuid,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion);
}
