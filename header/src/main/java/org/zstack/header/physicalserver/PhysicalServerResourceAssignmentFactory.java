package org.zstack.header.physicalserver;

public interface PhysicalServerResourceAssignmentFactory {
    PhysicalServerRoleType getRoleType();

    RoleServiceManifest roleServices();

    PhysicalServerResourceAssignmentObserver getResourceAssignment(String serverUuid);
}
