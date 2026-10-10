package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"inventory"})
public class APIGetMemoryPolicyReply extends APIReply {
    private MemoryPolicyInventory inventory;
    public MemoryPolicyInventory getInventory() { return inventory; }
    public void setInventory(MemoryPolicyInventory value) { inventory = value; }

    public static APIGetMemoryPolicyReply __example__() {
        APIGetMemoryPolicyReply reply = new APIGetMemoryPolicyReply();
        reply.setInventory(MemoryPolicyInventory.__example__());
        return reply;
    }
}
