package org.zstack.kvm.memory;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryHostMemoryStateReply extends APIQueryReply {
    private List<HostMemoryStateInventory> inventories;
    public List<HostMemoryStateInventory> getInventories() { return inventories; }
    public void setInventories(List<HostMemoryStateInventory> value) { inventories = value; }

    public static APIQueryHostMemoryStateReply __example__() {
        APIQueryHostMemoryStateReply reply = new APIQueryHostMemoryStateReply();
        reply.setInventories(Collections.singletonList(HostMemoryStateInventory.__example__()));
        return reply;
    }
}
