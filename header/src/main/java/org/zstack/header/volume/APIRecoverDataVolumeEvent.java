package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 11/12/2015.
 */
public class APIRecoverDataVolumeEvent extends APIEvent {
    private VolumeInventory inventory;

    public APIRecoverDataVolumeEvent() {
    }

    public APIRecoverDataVolumeEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
