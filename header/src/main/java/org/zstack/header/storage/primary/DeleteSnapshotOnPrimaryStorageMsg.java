package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 */
public class DeleteSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements DeleteOnPrimaryStorageMessage {
    private VolumeSnapshotInventory snapshot;
    private boolean gcOnFailure = false;

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

    @Override
    public boolean isGcOnFailure() {
        return gcOnFailure;
    }

    public void setGcOnFailure(boolean gcOnFailure) {
        this.gcOnFailure = gcOnFailure;
    }
}
