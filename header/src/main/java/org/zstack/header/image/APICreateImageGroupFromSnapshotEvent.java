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

    public static APICreateImageGroupFromSnapshotEvent __example__() {
        APICreateImageGroupFromSnapshotEvent event = new APICreateImageGroupFromSnapshotEvent();
        ImageGroupInventory inv = new ImageGroupInventory();
        inv.setUuid("a4b149e0-53b3-4c7e-b7fe-694b182eb001");
        inv.setName("example-image-group-from-snapshot");
        event.setInventory(inv);
        return event;
    }
}
