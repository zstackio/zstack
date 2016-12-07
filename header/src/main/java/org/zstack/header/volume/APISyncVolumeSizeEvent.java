package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/4/24.
 */
@RestResponse(allTo = "inventory")
public class APISyncVolumeSizeEvent extends APIEvent {
    private VolumeInventory inventory;

    public APISyncVolumeSizeEvent() {
    }

    public APISyncVolumeSizeEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
