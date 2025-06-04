package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateImageGroupFromImageEvent  extends APIEvent {
    ImageGroupInventory inventory;

    public APICreateImageGroupFromImageEvent(String apiId) {
        super(apiId);
    }

    public APICreateImageGroupFromImageEvent() {
        super();
    }

    public void setInventory(ImageGroupInventory imageGroup) {
        this.inventory = imageGroup;
    }

    public ImageGroupInventory getInventory() {
        return inventory;
    }
}
