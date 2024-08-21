package org.zstack.kvm;

import org.zstack.header.host.HostHardware;

public interface KvmHardwareStatusHandlerExtensionPoint {
    void handleKvmHardwareStatus(HostHardware hostHardwareType, KVMAgentCommands.HostPhysicalDeviceStatusAlarmEventCmd cmd);
}
