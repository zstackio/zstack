package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 1/4/2016.
 */
@RestResponse(allTo = "inventory")
public class APIDetachNetworkServiceFromL3NetworkEvent extends APIEvent {
    private L3NetworkInventory inventory;

    public APIDetachNetworkServiceFromL3NetworkEvent() {
    }

    public APIDetachNetworkServiceFromL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
}
