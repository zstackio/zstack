package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;

/**
 * Created by xing5 on 2016/4/24.
 */
public class APISyncVolumeActualSizeEvent extends APIEvent {
    private VolumeInventory inventory;

    public APISyncVolumeActualSizeEvent() {
    }

    public APISyncVolumeActualSizeEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
