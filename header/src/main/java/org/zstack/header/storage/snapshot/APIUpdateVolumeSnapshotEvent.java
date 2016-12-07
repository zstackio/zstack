package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVolumeSnapshotEvent extends APIEvent {
    private VolumeSnapshotInventory inventory;

    public APIUpdateVolumeSnapshotEvent() {
    }

    public APIUpdateVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public VolumeSnapshotInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
}
