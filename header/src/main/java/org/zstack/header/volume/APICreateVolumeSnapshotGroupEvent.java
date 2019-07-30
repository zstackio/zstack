package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestResponse(allTo = "inventory")
public class APICreateVolumeSnapshotGroupEvent extends APIEvent {
    private VolumeSnapshotGroupInventory inventory;

    public VolumeSnapshotGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateVolumeSnapshotGroupEvent(String apiId) {
        super(apiId);
    }

    public APICreateVolumeSnapshotGroupEvent() {
        super();
    }
}
