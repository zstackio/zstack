package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateNetworkSecurityPolicyScheduleEvent extends APIEvent {
    private NetworkSecurityPolicyScheduleInventory inventory;

    public APICreateNetworkSecurityPolicyScheduleEvent() {
    }

    public APICreateNetworkSecurityPolicyScheduleEvent(String apiId) {
        super(apiId);
    }

    public NetworkSecurityPolicyScheduleInventory getInventory() {
        return inventory;
    }

    public void setInventory(NetworkSecurityPolicyScheduleInventory inventory) {
        this.inventory = inventory;
    }
}
