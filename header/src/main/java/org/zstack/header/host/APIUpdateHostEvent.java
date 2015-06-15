package org.zstack.header.host;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/14/2015.
 */
public class APIUpdateHostEvent extends APIEvent {
    private HostInventory inventory;

    public APIUpdateHostEvent() {
    }

    public APIUpdateHostEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
}
