package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by boce.wang on 10/24/2024.
 */
@RestResponse(allTo = "inventory")
public class APICreateHostNetworkServiceTypeEvent extends APIEvent {
    private HostNetworkLabelInventory inventory;

    public APICreateHostNetworkServiceTypeEvent(String apiId) {
        super(apiId);
    }

    public APICreateHostNetworkServiceTypeEvent() {
        super(null);
    }

    public HostNetworkLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkLabelInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateHostNetworkServiceTypeEvent __example__() {
        APICreateHostNetworkServiceTypeEvent event = new APICreateHostNetworkServiceTypeEvent();
        event.setInventory(new HostNetworkLabelInventory());
        return event;
    }
}
