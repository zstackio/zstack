package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Map;

@RestResponse(fieldsTo = {"inventories"})
public class APIGetVmMemoryOptimizationsReply extends APIReply {
    private Map<String, MemoryVmAccountingInventory> inventories;
    public Map<String, MemoryVmAccountingInventory> getInventories() { return inventories; }
    public void setInventories(Map<String, MemoryVmAccountingInventory> value) { inventories = value; }
    public static APIGetVmMemoryOptimizationsReply __example__() {
        APIGetVmMemoryOptimizationsReply reply = new APIGetVmMemoryOptimizationsReply();
        MemoryVmAccountingInventory vm = MemoryVmAccountingInventory.__example__();
        Map<String, MemoryVmAccountingInventory> inventories = new java.util.LinkedHashMap<>();
        inventories.put(vm.getVmUuid(), vm);
        reply.setInventories(inventories);
        return reply;
    }
}
