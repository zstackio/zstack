package org.zstack.header.core.external.plugin;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetFeaturePluginPackagesReply extends APIReply {
    private List<FeaturePluginPackageInventory> inventories;

    public List<FeaturePluginPackageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FeaturePluginPackageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetFeaturePluginPackagesReply __example__() {
        APIGetFeaturePluginPackagesReply reply = new APIGetFeaturePluginPackagesReply();
        reply.setInventories(Collections.singletonList(FeaturePluginPackageInventory.__example__()));
        return reply;
    }
}
