package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.L3NetworkInventory;

public interface VmSetStaticIpExtensionPoint {
    boolean accepts(VmInstanceInventory vm, VmNicInventory nic, L3NetworkInventory l3);

    void setStaticIp(SetVmStaticIpMsg msg, VmInstanceSpec spec, VmNicInventory nic,
                     L3NetworkInventory l3, Completion completion);
}
