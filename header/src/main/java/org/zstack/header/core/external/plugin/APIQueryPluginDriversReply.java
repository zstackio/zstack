package org.zstack.header.core.external.plugin;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryPluginDriversReply extends APIQueryReply {
    private List<PluginDriverInventory> inventories;

    public List<PluginDriverInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PluginDriverInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPluginDriversReply __example__() {
        APIQueryPluginDriversReply reply = new APIQueryPluginDriversReply();
        PluginDriverInventory inv = new PluginDriverInventory();
        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}
