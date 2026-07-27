package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateNetworkSecurityPolicyScheduleEvent extends APIEvent {
    private NetworkSecurityPolicyScheduleInventory inventory;

    public APIUpdateNetworkSecurityPolicyScheduleEvent() {
    }

    public APIUpdateNetworkSecurityPolicyScheduleEvent(String apiId) {
        super(apiId);
    }

    public NetworkSecurityPolicyScheduleInventory getInventory() {
        return inventory;
    }

    public void setInventory(NetworkSecurityPolicyScheduleInventory inventory) {
        this.inventory = inventory;
    }
}
