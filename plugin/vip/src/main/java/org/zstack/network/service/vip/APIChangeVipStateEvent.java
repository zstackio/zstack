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
}
