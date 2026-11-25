package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APIGetHostMemorySwapDisksReply extends APIReply {
    private MemorySwapDiskInventory inventory;

    public MemorySwapDiskInventory getInventory() { return inventory; }
    public void setInventory(MemorySwapDiskInventory inventory) { this.inventory = inventory; }

    public static APIGetHostMemorySwapDisksReply __example__() {
        APIGetHostMemorySwapDisksReply reply = new APIGetHostMemorySwapDisksReply();
        reply.setInventory(MemorySwapDiskInventory.__example__());
        return reply;
    }
}
