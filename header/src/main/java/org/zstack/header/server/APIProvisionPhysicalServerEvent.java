package org.zstack.header.server;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIProvisionPhysicalServerEvent extends APIEvent {
    private LongJobInventory inventory;

    public APIProvisionPhysicalServerEvent() {
    }

    public APIProvisionPhysicalServerEvent(String apiId) {
        super(apiId);
    }

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public static APIProvisionPhysicalServerEvent __example__() {
        APIProvisionPhysicalServerEvent event = new APIProvisionPhysicalServerEvent();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        event.setInventory(inv);
        return event;
    }
}
