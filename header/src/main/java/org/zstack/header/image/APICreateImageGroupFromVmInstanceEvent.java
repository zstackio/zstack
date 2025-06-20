package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateImageGroupFromVmInstanceEvent extends APIEvent {
    ImageGroupInventory inventory;

    public APICreateImageGroupFromVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APICreateImageGroupFromVmInstanceEvent() {
        super();
    }

    public void setInventory(ImageGroupInventory imageGroup) {
        this.inventory = imageGroup;
    }

    public ImageGroupInventory getInventory() {
        return inventory;
    }
}
