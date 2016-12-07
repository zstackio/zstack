package org.zstack.network.service.vip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVipEvent extends APIEvent {
    private VipInventory inventory;

    public VipInventory getInventory() {
        return inventory;
    }

    public void setInventory(VipInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateVipEvent() {
    }

    public APIUpdateVipEvent(String apiId) {
        super(apiId);
    }
}
