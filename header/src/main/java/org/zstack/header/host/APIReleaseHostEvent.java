package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIReleaseHostEvent extends APIEvent {
    private HostInventory inventory;

    public APIReleaseHostEvent() {
        super(null);
    }

    public APIReleaseHostEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    public static APIReleaseHostEvent __example__() {
        APIReleaseHostEvent event = new APIReleaseHostEvent();
        HostInventory inv = new HostInventory();
        inv.setName("host-1");
        inv.setUuid(uuid());
        inv.setState(HostState.Enabled.toString());
        inv.setStatus(HostStatus.Connected.toString());
        event.setInventory(inv);
        event.setSuccess(true);
        return event;
    }
}