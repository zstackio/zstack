package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;

/**
 * Created by MaJin on 2019/7/10.
 */
public class DeleteVolumeSnapshotGroupInnerMsg extends NeedReplyMessage implements VolumeSnapshotGroupMessage {
    private String uuid;
    private String deletionMode;
    private boolean onlySelf;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    @Override
    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_DELETION;
    }

    public void setDeletionMode(APIDeleteMessage.DeletionMode deletionMode) {
        this.deletionMode = deletionMode.toString();
    }

    public APIDeleteMessage.DeletionMode getDeletionMode() {
        return APIDeleteMessage.DeletionMode.valueOf(deletionMode);
    }

    public boolean isOnlySelf() {
        return onlySelf;
    }

    public void setOnlySelf(boolean onlySelf) {
        this.onlySelf = onlySelf;
    }
}
