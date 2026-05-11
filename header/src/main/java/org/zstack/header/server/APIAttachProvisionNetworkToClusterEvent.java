package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAttachProvisionNetworkToClusterEvent extends APIEvent {
    private PhysicalServerProvisionNetworkInventory inventory;

    public APIAttachProvisionNetworkToClusterEvent() {}
    public APIAttachProvisionNetworkToClusterEvent(String apiId) { super(apiId); }

    public PhysicalServerProvisionNetworkInventory getInventory() { return inventory; }
    public void setInventory(PhysicalServerProvisionNetworkInventory inventory) { this.inventory = inventory; }

    public static APIAttachProvisionNetworkToClusterEvent __example__() {
        return new APIAttachProvisionNetworkToClusterEvent();
    }
}
