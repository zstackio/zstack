package org.zstack.header.vm.devices;

import java.util.List;

public interface VmDeviceProvider {
    List<? extends VmDevice> getAttachedDevices(String vmUuid);
}
