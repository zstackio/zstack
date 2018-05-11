package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by kayo on 2018/5/9.
 */
public class DeleteVolumeSnapshotMsg extends NeedReplyMessage implements VolumeSnapshotMessage {
    private APIDeleteVolumeSnapshotMsg apiMessage;
    private String snapshotUuid;
    private String volumeUuid;
    private String treeUuid;

    public APIDeleteVolumeSnapshotMsg getApiMessage() {
        return apiMessage;
    }

    public void setApiMessage(APIDeleteVolumeSnapshotMsg apiMessage) {
        this.apiMessage = apiMessage;
    }

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
