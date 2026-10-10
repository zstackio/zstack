package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APIGetHostMemoryWritebackBackendsReply extends APIReply {
    private MemoryWritebackBackendInventory inventory;

    public MemoryWritebackBackendInventory getInventory() { return inventory; }
    public void setInventory(MemoryWritebackBackendInventory inventory) { this.inventory = inventory; }

    public static APIGetHostMemoryWritebackBackendsReply __example__() {
        APIGetHostMemoryWritebackBackendsReply reply = new APIGetHostMemoryWritebackBackendsReply();
        reply.setInventory(MemoryWritebackBackendInventory.__example__());
        return reply;
    }
}
