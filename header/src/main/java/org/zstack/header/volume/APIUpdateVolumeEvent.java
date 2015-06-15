package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 6/14/2015.
 */
public class APIUpdateVolumeEvent extends APIEvent {
    private VolumeInventory inventory;

    public APIUpdateVolumeEvent() {
    }

    public APIUpdateVolumeEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
