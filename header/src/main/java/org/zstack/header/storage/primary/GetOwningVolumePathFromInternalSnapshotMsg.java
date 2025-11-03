package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class GetOwningVolumePathFromInternalSnapshotMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private List<String> snapshotPaths;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<String> getSnapshotPaths() {
        return snapshotPaths;
    }

    public void setSnapshotPaths(List<String> snapshotPaths) {
        this.snapshotPaths = snapshotPaths;
    }
}
