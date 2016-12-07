package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateRootVolumeTemplateFromRootVolumeEvent extends APIEvent {
    private ImageInventory inventory;

    public APICreateRootVolumeTemplateFromRootVolumeEvent(String apiId) {
        super(apiId);
    }

    public APICreateRootVolumeTemplateFromRootVolumeEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
