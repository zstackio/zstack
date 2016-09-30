package org.zstack.header.storage.primary;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotStruct;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;

/**
 */
@ApiTimeout(apiClasses = {APICreateVolumeSnapshotMsg.class})
public class TakeSnapshotMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private VolumeSnapshotStruct struct;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public VolumeSnapshotStruct getStruct() {
        return struct;
    }

    public void setStruct(VolumeSnapshotStruct struct) {
        this.struct = struct;
    }
}
