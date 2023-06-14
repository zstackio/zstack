package org.zstack.physicalNetworkInterface.header;

import org.zstack.pciDevice.PciDeviceStatus;

import java.util.List;

import static java.util.Arrays.asList;

public enum HostVirtualNetworkInterfaceStatus {
    System,
    Attached,
    ;

    public static List<HostVirtualNetworkInterfaceStatus> attachablePciDeviceStatus = asList(System);
    public boolean isAttachable() {
        return attachablePciDeviceStatus.contains(this);
    }
}
