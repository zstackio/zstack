package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIAddIpRangeByNetworkCidrEvent extends APIEvent {
    private IpRangeInventory inventory;

    public IpRangeInventory getInventory() {
        return inventory;
    }

    public void setInventory(IpRangeInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddIpRangeByNetworkCidrEvent(String apiId) {
        super(apiId);
    }

    public APIAddIpRangeByNetworkCidrEvent() {
        super(null);
    }
}
