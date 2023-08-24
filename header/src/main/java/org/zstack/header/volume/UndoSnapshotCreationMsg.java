package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

public class UndoSnapshotCreationMsg extends NeedReplyMessage implements VolumeMessage {
    String vmInstanceUuid;
    String volumeUuid;
    VolumeSnapshotInventory snapShot;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public VolumeSnapshotInventory getSnapShot() {
        return snapShot;
    }

    public void setSnapShot(VolumeSnapshotInventory snapShot) {
        this.snapShot = snapShot;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
