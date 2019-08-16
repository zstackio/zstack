package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;

import java.util.List;

/**
 * Created by MaJin on 2019/8/16.
 */
public class CheckVolumeSnapshotOperationOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private List<String> volumeSnapshotUuids;
    private String primaryStorageUuid;
    private SnapshotBackendOperation operation;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public List<String> getVolumeSnapshotUuids() {
        return volumeSnapshotUuids;
    }

    public void setVolumeSnapshotUuids(List<String> volumeSnapshotUuids) {
        this.volumeSnapshotUuids = volumeSnapshotUuids;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public SnapshotBackendOperation getOperation() {
        return operation;
    }

    public void setOperation(SnapshotBackendOperation operation) {
        this.operation = operation;
    }
}
