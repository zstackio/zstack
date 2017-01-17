package org.zstack.core.config;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryGlobalConfigReply extends APIQueryReply {
    private List<GlobalConfigInventory> inventories;

    public List<GlobalConfigInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GlobalConfigInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryGlobalConfigReply __example__() {
        APIQueryGlobalConfigReply reply = new APIQueryGlobalConfigReply();


        return reply;
    }

}
