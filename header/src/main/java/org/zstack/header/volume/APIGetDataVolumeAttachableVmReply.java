package org.zstack.header.volume;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetDataVolumeAttachableVmReply extends APIReply {
    private List<VmInstanceInventory> inventories;

    public List<VmInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceInventory> inventories) {
        this.inventories = inventories;
    }
}
