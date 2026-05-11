package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeClusterServerPoolEvent extends APIEvent {
    private ServerPoolInventory inventory;

    public APIChangeClusterServerPoolEvent() {}
    public APIChangeClusterServerPoolEvent(String apiId) { super(apiId); }

    public ServerPoolInventory getInventory() { return inventory; }
    public void setInventory(ServerPoolInventory inventory) { this.inventory = inventory; }

    public static APIChangeClusterServerPoolEvent __example__() {
        APIChangeClusterServerPoolEvent evt = new APIChangeClusterServerPoolEvent();
        return evt;
    }
}
