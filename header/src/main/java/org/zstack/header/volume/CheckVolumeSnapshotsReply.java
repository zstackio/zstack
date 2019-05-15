package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2019/4/10.
 */
public class CheckVolumeSnapshotsReply extends MessageReply {
    private boolean completed;
    private String snapshotUuid;

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
