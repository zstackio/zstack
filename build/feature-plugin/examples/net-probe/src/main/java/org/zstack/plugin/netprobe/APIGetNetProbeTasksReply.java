package org.zstack.plugin.netprobe;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetNetProbeTasksReply extends APIReply {
    private List<NetProbeTaskInventory> inventories;

    public List<NetProbeTaskInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetProbeTaskInventory> inventories) {
        this.inventories = inventories;
    }
}
