package org.zstack.network.service.vip;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/15/2015.
 */
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
