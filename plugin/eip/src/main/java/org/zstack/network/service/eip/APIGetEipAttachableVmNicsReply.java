package org.zstack.network.service.eip;

import org.zstack.header.message.APIReply;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 */
public class APIGetEipAttachableVmNicsReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
}
