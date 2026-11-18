package org.zstack.header.physicalserver;

import org.zstack.header.core.ReturnValueCompletion;
import java.util.List;

public interface PhysicalServerResourceAssignmentObserver {
    PhysicalServerRoleType getRoleType();

    boolean resourceExists();

    void collectResourceAssignment(String serverUuid, List<String> serviceNames,
            ReturnValueCompletion<PhysicalServerResourceBoundary> completion);
}
