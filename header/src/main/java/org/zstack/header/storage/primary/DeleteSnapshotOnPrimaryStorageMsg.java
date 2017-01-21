package org.zstack.header.storage.primary;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.APIExpungeVmInstanceMsg;

/**
 */
@ApiTimeout(apiClasses = {APIDeleteVolumeSnapshotMsg.class, APIExpungeVmInstanceMsg.class})
public class DeleteSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private VolumeSnapshotInventory snapshot;

    @Override
    public String getPrimaryStorageUuid() {
        return snapshot.getPrimaryStorageUuid();
    }

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }
}
