package org.zstack.sdk;

import org.zstack.sdk.NetworkSecurityPolicyScheduleInventory;

public class CreateNetworkSecurityPolicyScheduleResult {
    public NetworkSecurityPolicyScheduleInventory inventory;
    public void setInventory(NetworkSecurityPolicyScheduleInventory inventory) {
        this.inventory = inventory;
    }
    public NetworkSecurityPolicyScheduleInventory getInventory() {
        return this.inventory;
    }

}
