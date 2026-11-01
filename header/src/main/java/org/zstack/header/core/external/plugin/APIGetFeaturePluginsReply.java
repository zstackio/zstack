package org.zstack.header.core.external.plugin;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetFeaturePluginsReply extends APIReply {
    private List<FeaturePluginInventory> inventories;

    public List<FeaturePluginInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FeaturePluginInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetFeaturePluginsReply __example__() {
        APIGetFeaturePluginsReply reply = new APIGetFeaturePluginsReply();
        FeaturePluginInventory inv = new FeaturePluginInventory();
        inv.setId("flowMeter");
        inv.setName("flowMeter");
        inv.setCategory("network");
        inv.setVersion("5.5.48-1");
        inv.setStatus(FeaturePluginInventory.STATUS_LOADED);
        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}
