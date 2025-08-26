package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateImageGroupFromImageEvent extends APIEvent {
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

    public static APICreateImageGroupFromImageEvent __example__() {
        APICreateImageGroupFromImageEvent event = new APICreateImageGroupFromImageEvent();
        ImageGroupInventory inv = new ImageGroupInventory();
        inv.setUuid("f0b149e0-53b3-4c7e-b7fe-694b182ebffd");
        inv.setName("example-image-group");
        inv.setDescription("example image group");
        event.setInventory(inv);
        return event;
    }
}
