package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2019/4/11.
 */
public class CheckVolumeSnapshotsOnPrimaryStorageReply extends MessageReply {
    private String snapshotUuid;
    private boolean completed;

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
