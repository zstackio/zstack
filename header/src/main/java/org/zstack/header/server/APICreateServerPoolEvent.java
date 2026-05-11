package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateServerPoolEvent extends APIEvent {
    private ServerPoolInventory inventory;

    public APICreateServerPoolEvent() {
        super(null);
    }

    public APICreateServerPoolEvent(String apiId) {
        super(apiId);
    }

    public ServerPoolInventory getInventory() {
        return inventory;
    }

    public void setInventory(ServerPoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateServerPoolEvent __example__() {
        APICreateServerPoolEvent event = new APICreateServerPoolEvent();
        ServerPoolInventory inv = new ServerPoolInventory();
        inv.setUuid(uuid());
        inv.setName("pool-rack-A1");
        inv.setState("Enabled");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
