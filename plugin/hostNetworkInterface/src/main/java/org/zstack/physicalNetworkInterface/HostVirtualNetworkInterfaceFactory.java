package org.zstack.physicalNetworkInterface;

import org.zstack.header.core.Completion;
import org.zstack.physicalNetworkInterface.header.HostNetworkInterfaceInventory;

public interface HostVirtualNetworkInterfaceFactory {
    String getActionType();
    void generateHostVirtualNetworkInterfaces(HostNetworkInterfaceInventory hostNetworkInterfaceInventory, Integer virtPartNum,Completion completion);
    void ungenerateHostVirtualNetworkInterface(HostNetworkInterfaceInventory hostNetworkInterfaceInventory, Completion completion);

    SplitHostNetworkInterfaceStruct getSplitHostNetworkInterfaceStruct(HostNetworkInterfaceInventory hostNetworkInterfaceInventory);
}
