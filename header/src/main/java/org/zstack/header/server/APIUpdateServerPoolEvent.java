package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIUpdateServerPoolEvent extends APIEvent {
    private ServerPoolInventory inventory;

    public APIUpdateServerPoolEvent() {
        super(null);
    }

    public APIUpdateServerPoolEvent(String apiId) {
        super(apiId);
    }

    public ServerPoolInventory getInventory() {
        return inventory;
    }

    public void setInventory(ServerPoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateServerPoolEvent __example__() {
        APIUpdateServerPoolEvent event = new APIUpdateServerPoolEvent();
        ServerPoolInventory inv = new ServerPoolInventory();
        inv.setUuid(uuid());
        inv.setName("pool-updated");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
