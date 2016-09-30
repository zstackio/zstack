package org.zstack.header.vm;

import org.zstack.header.network.l3.UsedIpInventory;

/**
 * Created by xing5 on 2016/4/20.
 */
public interface VmIpChangedExtensionPoint {
    void vmIpChanged(VmInstanceInventory vm, VmNicInventory nic, UsedIpInventory oldIp, UsedIpInventory newIp);
}
