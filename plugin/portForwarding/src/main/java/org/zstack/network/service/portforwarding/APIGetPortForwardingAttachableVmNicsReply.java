package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIReply;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 */
public class APIGetPortForwardingAttachableVmNicsReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
}
