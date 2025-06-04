package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateImageGroupFromSnapshotEvent extends APIEvent {
    ImageGroupInventory inventory;

    public APICreateImageGroupFromSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APICreateImageGroupFromSnapshotEvent() {
        super();
    }

    public void setInventory(ImageGroupInventory imageGroup) {
        this.inventory = imageGroup;
    }

    public ImageGroupInventory getInventory() {
        return inventory;
    }

}
