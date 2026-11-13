package org.zstack.header.vm.devices;

public interface VmDevice {
    String getUuid();
    VmDeviceMigrationCapability getMigrationCapability();
    String getMigrationErrorCode();
}
