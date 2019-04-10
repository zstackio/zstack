package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2019/4/10.
 */
public class CheckVolumeSnapshotsMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;
    private String currentTreeSnapshotInstallPath;
    private List<VolumeSnapshotInventory> snapshots = new ArrayList<>();

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getCurrentTreeSnapshotInstallPath() {
        return currentTreeSnapshotInstallPath;
    }

    public void setCurrentTreeSnapshotInstallPath(String currentTreeSnapshotInstallPath) {
        this.currentTreeSnapshotInstallPath = currentTreeSnapshotInstallPath;
    }

    public List<VolumeSnapshotInventory> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<VolumeSnapshotInventory> snapshots) {
        this.snapshots = snapshots;
    }
}
