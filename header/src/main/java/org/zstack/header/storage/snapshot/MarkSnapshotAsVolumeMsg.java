package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:35 2023/9/4
 */
public class MarkSnapshotAsVolumeMsg extends NeedReplyMessage implements VolumeSnapshotMessage {
    private String volumeUuid;
    private String snapshotUuid;
    private long size;
    private String volumePath;
    private String treeUuid;

    public String getVolumePath() {
        return volumePath;
    }

    public void setVolumePath(String volumePath) {
        this.volumePath = volumePath;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

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

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
