package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Created by boce.wang on 11/29/2023.
 */
public interface VmUpdateNicExtensionPoint {
    void beforeUpdateNic(VmInstanceSpec spec, VmNicInventory nic, L3NetworkInventory destL3);

    void afterUpdateNic(VmInstanceSpec spec, VmNicInventory nic, L3NetworkInventory destL3);
}
