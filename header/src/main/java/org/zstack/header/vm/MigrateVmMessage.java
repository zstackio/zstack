package org.zstack.header.vm;

import org.zstack.header.allocator.AllocationScene;

public interface MigrateVmMessage {
    String getHostUuid();
    String getStrategy();
    Integer getDownTime();
    boolean isMigrateFromDestination();
    boolean isAllowUnknown();

    default VmMigrationMode getMigrationMode() {
        return VmMigrationMode.Live;
    }

    default AllocationScene getAllocationScene() {
        return null;
    }
}
