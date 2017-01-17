package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateDataVolumeFromVolumeTemplateEvent extends APIEvent {
    private VolumeInventory inventory;

    public APICreateDataVolumeFromVolumeTemplateEvent() {
    }

    public APICreateDataVolumeFromVolumeTemplateEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateDataVolumeFromVolumeTemplateEvent __example__() {
        APICreateDataVolumeFromVolumeTemplateEvent event = new APICreateDataVolumeFromVolumeTemplateEvent();


        return event;
    }

}
