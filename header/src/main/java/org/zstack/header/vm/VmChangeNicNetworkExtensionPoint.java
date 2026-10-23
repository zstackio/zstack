package org.zstack.header.vm;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.network.l3.L3NetworkInventory;

public interface VmChangeNicNetworkExtensionPoint {
    boolean accepts(VmInstanceInventory vm, VmNicInventory nic, L3NetworkInventory destination);

    void changeNetwork(ChangeVmNicNetworkMsg msg, VmInstanceSpec spec, VmNicInventory nic,
                       L3NetworkInventory destination, ReturnValueCompletion<VmNicInventory> completion);
}
