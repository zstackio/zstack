package org.zstack.header.network.service;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryNetworkServiceProviderReply extends APIQueryReply {
    private List<NetworkServiceProviderInventory> inventories;

    public List<NetworkServiceProviderInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkServiceProviderInventory> inventories) {
        this.inventories = inventories;
    }
}
