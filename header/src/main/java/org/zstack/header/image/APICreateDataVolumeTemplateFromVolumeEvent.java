package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateDataVolumeTemplateFromVolumeEvent extends APIEvent {
    private ImageInventory inventory;

    public APICreateDataVolumeTemplateFromVolumeEvent(String apiId) {
        super(apiId);
    }

    public APICreateDataVolumeTemplateFromVolumeEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
