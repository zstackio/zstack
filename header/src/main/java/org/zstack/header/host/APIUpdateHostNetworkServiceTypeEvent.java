package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by boce.wang on 10/25/2024.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateHostNetworkServiceTypeEvent extends APIEvent {
    private HostNetworkLabelInventory inventory;

    public APIUpdateHostNetworkServiceTypeEvent() {

    }

    public APIUpdateHostNetworkServiceTypeEvent(String apiId) {
        super(apiId);
    }

    public HostNetworkLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkLabelInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateHostNetworkServiceTypeEvent __example__() {
        APIUpdateHostNetworkServiceTypeEvent event = new APIUpdateHostNetworkServiceTypeEvent();
        event.setInventory(new HostNetworkLabelInventory());
        event.setSuccess(true);
        return event;
    }
}
