package org.zstack.header.vm.devices;

import org.zstack.header.vm.VmMigrationMode;

public enum VmDeviceMigrationCapability {
    None,
    StopAndStart,
    LiveOnly,
    Live;

    public boolean supports(VmMigrationMode mode) {
        return this == Live || this == StopAndStart && mode == VmMigrationMode.StopAndStart
                || this == LiveOnly && mode == VmMigrationMode.Live;
    }
}
