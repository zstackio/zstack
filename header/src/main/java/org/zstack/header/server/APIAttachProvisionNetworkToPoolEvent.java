package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAttachProvisionNetworkToPoolEvent extends APIEvent {
    private PhysicalServerProvisionNetworkInventory inventory;

    public APIAttachProvisionNetworkToPoolEvent() {}
    public APIAttachProvisionNetworkToPoolEvent(String apiId) { super(apiId); }

    public PhysicalServerProvisionNetworkInventory getInventory() { return inventory; }
    public void setInventory(PhysicalServerProvisionNetworkInventory inventory) { this.inventory = inventory; }

    public static APIAttachProvisionNetworkToPoolEvent __example__() {
        return new APIAttachProvisionNetworkToPoolEvent();
    }
}
