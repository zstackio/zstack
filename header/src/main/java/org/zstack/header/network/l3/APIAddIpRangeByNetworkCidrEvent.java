package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
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
