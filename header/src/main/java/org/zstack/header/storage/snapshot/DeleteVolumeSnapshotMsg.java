package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by kayo on 2018/5/9.
 */
public class DeleteVolumeSnapshotMsg extends NeedReplyMessage implements DeleteVolumeSnapshotMessage {
    private String snapshotUuid;
    private String volumeUuid;
    private String treeUuid;
    private String deletionMode;

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    @Override
    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setDeletionMode(APIDeleteMessage.DeletionMode deletionMode) {
        this.deletionMode = deletionMode.toString();
    }

    @Override
    public APIDeleteMessage.DeletionMode getDeletionMode() {
        return APIDeleteMessage.DeletionMode.valueOf(deletionMode);
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }
}
