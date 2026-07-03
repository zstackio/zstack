package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeInventory;

@RestResponse(allTo = "inventory")
public class APIChangeVolumeProtocolEvent extends APIEvent {
    private VolumeInventory inventory;

    public APIChangeVolumeProtocolEvent() {
    }

    public APIChangeVolumeProtocolEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeVolumeProtocolEvent __example__() {
        APIChangeVolumeProtocolEvent event = new APIChangeVolumeProtocolEvent();
        return event;
    }
}
