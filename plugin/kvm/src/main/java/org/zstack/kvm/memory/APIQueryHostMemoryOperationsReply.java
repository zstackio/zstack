package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APIQueryHostMemoryOperationsReply extends APIReply {
    private MemoryHostOperationsInventory inventory;
    public MemoryHostOperationsInventory getInventory() { return inventory; }
    public void setInventory(MemoryHostOperationsInventory value) { inventory = value; }
    public static APIQueryHostMemoryOperationsReply __example__() {
        APIQueryHostMemoryOperationsReply reply = new APIQueryHostMemoryOperationsReply();
        reply.setInventory(MemoryHostOperationsInventory.__example__());
        return reply;
    }
}
