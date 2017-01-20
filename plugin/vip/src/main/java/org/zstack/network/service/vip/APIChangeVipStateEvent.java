package org.zstack.network.service.vip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeVipStateEvent extends APIEvent {
    private VipInventory inventory;

    public APIChangeVipStateEvent() {
        super(null);
    }

    public APIChangeVipStateEvent(String apiId) {
        super(apiId);
    }

    public VipInventory getInventory() {
        return inventory;
    }

    public void setInventory(VipInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeVipStateEvent __example__() {
        APIChangeVipStateEvent event = new APIChangeVipStateEvent();
        VipInventory inventory = new VipInventory();
        inventory.setName("vip1");
        inventory.setL3NetworkUuid(uuid());
        inventory.setUuid(uuid());
        inventory.setGateway("127.0.0.1");
        inventory.setNetmask("255.255.0.0");
        inventory.setIp("192.168.0.1");
        inventory.setIpRangeUuid(uuid());
        inventory.setPeerL3NetworkUuid(uuid());
        inventory.setState("enable");

        event.setInventory(inventory);

        return event;
    }

}
