package org.zstack.header.physicalserver;

import org.zstack.header.core.ReturnValueCompletion;

public interface PhysicalServerResourceAssignmentObserver {
    PhysicalServerRoleType getRoleType();

    default PhysicalServerResourceIsolationMode getIsolationMode() {
        return PhysicalServerResourceIsolationMode.SHARED;
    }

    void collectResourceAssignment(String serverUuid, ReturnValueCompletion<PhysicalServerResourceBoundary> completion);
}
