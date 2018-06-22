package org.zstack.header.storage.snapshot;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;
import org.zstack.header.volume.VolumeMessage;

import java.util.List;
import java.util.Map;

/**
 * Create by weiwang at 2018/6/11
 */
@ApiTimeout(apiClasses = {APICreateVolumeSnapshotMsg.class})
public class CreateVolumesSnapshotOverlayInnerMsg extends NeedReplyMessage implements VolumeMessage {
    private String accountUuid;

    private Boolean atomic = true;

    private List<CreateVolumesSnapshotsJobStruct> volumeSnapshotJobs;

    private List<String> lockedVmInstanceUuids;

    private List<String> lockedVolumeUuids;

    public List<String> getLockedVmInstanceUuids() {
        return lockedVmInstanceUuids;
    }

    public void setLockedVmInstanceUuids(List<String> lockedVmInstanceUuids) {
        this.lockedVmInstanceUuids = lockedVmInstanceUuids;
    }

    public List<String> getLockedVolumeUuids() {
        return lockedVolumeUuids;
    }

    public void setLockedVolumeUuids(List<String> lockedVolumeUuids) {
        this.lockedVolumeUuids = lockedVolumeUuids;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public List<CreateVolumesSnapshotsJobStruct> getVolumeSnapshotJobs() {
        return volumeSnapshotJobs;
    }

    public void setVolumeSnapshotJobs(List<CreateVolumesSnapshotsJobStruct> volumeSnapshotJobs) {
        this.volumeSnapshotJobs = volumeSnapshotJobs;
    }

    public Boolean getAtomic() {
        return atomic;
    }

    public void setAtomic(Boolean atomic) {
        this.atomic = atomic;
    }

    @Override
    public String getVolumeUuid() {
        return volumeSnapshotJobs.get(0).getVolumeUuid();
    }
}
