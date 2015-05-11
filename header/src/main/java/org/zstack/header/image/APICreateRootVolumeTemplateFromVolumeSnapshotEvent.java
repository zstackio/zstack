package org.zstack.header.image;

import org.zstack.header.message.APIEvent;

/**
 */
public class APICreateRootVolumeTemplateFromVolumeSnapshotEvent extends APIEvent {
    private ImageInventory inventory;

    public APICreateRootVolumeTemplateFromVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APICreateRootVolumeTemplateFromVolumeSnapshotEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
