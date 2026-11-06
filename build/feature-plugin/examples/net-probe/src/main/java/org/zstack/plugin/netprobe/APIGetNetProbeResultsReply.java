package org.zstack.plugin.netprobe;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetNetProbeResultsReply extends APIReply {
    private List<NetProbeResultInventory> inventories;

    public List<NetProbeResultInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetProbeResultInventory> inventories) {
        this.inventories = inventories;
    }
}
