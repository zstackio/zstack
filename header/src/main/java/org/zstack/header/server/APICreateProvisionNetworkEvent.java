package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateProvisionNetworkEvent extends APIEvent {
    private PhysicalServerProvisionNetworkInventory inventory;

    public APICreateProvisionNetworkEvent() { super(null); }
    public APICreateProvisionNetworkEvent(String apiId) { super(apiId); }

    public PhysicalServerProvisionNetworkInventory getInventory() { return inventory; }
    public void setInventory(PhysicalServerProvisionNetworkInventory inventory) { this.inventory = inventory; }

    public static APICreateProvisionNetworkEvent __example__() {
        APICreateProvisionNetworkEvent evt = new APICreateProvisionNetworkEvent();
        return evt;
    }
}
