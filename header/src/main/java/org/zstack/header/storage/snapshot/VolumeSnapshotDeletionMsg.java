package org.zstack.header.storage.snapshot;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.DeletionMessage;
import org.zstack.header.vm.APIExpungeVmInstanceMsg;

/**
 */
@ApiTimeout(apiClasses = {APIDeleteVolumeSnapshotMsg.class, APIExpungeVmInstanceMsg.class})
public class VolumeSnapshotDeletionMsg extends DeletionMessage implements VolumeSnapshotMessage {
    private String snapshotUuid;
    private String volumeUuid;
    private boolean volumeDeletion;
    /**
     * @ignore
     */
    private String treeUuid;

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    public boolean isVolumeDeletion() {
        return volumeDeletion;
    }

    public void setVolumeDeletion(boolean volumeDeletion) {
        this.volumeDeletion = volumeDeletion;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
