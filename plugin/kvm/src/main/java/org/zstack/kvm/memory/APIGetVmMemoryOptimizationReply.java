package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APIGetVmMemoryOptimizationReply extends APIReply {
    private MemoryVmAccountingInventory inventory;
    public MemoryVmAccountingInventory getInventory() { return inventory; }
    public void setInventory(MemoryVmAccountingInventory value) { inventory = value; }
    public static APIGetVmMemoryOptimizationReply __example__() {
        APIGetVmMemoryOptimizationReply reply = new APIGetVmMemoryOptimizationReply();
        reply.setInventory(MemoryVmAccountingInventory.__example__());
        return reply;
    }
}
