package org.zstack.header.storage.snapshot;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;

import java.util.List;

/**
 * Create by weiwang at 2018/6/12
 */
@ApiTimeout(apiClasses = {APICreateVolumeSnapshotMsg.class})
public class TakeVolumesSnapshotOnKvmMsg extends NeedReplyMessage implements HostMessage {
    private List<TakeSnapshotsOnKvmJobStruct> snapshotJobs;
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<TakeSnapshotsOnKvmJobStruct> getSnapshotJobs() {
        return snapshotJobs;
    }

    public void setSnapshotJobs(List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        this.snapshotJobs = snapshotJobs;
    }
}
