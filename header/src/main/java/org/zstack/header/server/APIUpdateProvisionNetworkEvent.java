package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIUpdateProvisionNetworkEvent extends APIEvent {
    private PhysicalServerProvisionNetworkInventory inventory;

    public APIUpdateProvisionNetworkEvent() {
        super(null);
    }

    public APIUpdateProvisionNetworkEvent(String apiId) {
        super(apiId);
    }

    public PhysicalServerProvisionNetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(PhysicalServerProvisionNetworkInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateProvisionNetworkEvent __example__() {
        APIUpdateProvisionNetworkEvent event = new APIUpdateProvisionNetworkEvent();
        PhysicalServerProvisionNetworkInventory inv = new PhysicalServerProvisionNetworkInventory();
        inv.setUuid(uuid());
        inv.setName("provision-net-updated");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
