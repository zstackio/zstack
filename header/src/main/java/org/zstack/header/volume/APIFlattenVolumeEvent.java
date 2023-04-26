package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIFlattenVolumeEvent extends APIEvent {
    private VolumeInventory inventory;

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public APIFlattenVolumeEvent() {
        super();
    }

    public APIFlattenVolumeEvent(String id) {
        super(id);
    }
}
