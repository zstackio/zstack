package org.zstack.header.physicalserver;

import org.zstack.header.core.ReturnValueCompletion;

public interface PhysicalServerResourceAssignmentObserver {
    String getRoleType();

    void collectResourceAssignment(
            String serverUuid,
            ReturnValueCompletion<PhysicalServerResourceBoundary> completion);
}
