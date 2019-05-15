package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2019/4/11.
 */
public class CheckVolumeSnapshotsOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String volumeUuid;
    private String volumeInstallPath;
    private List<VolumeSnapshotInventory> snapshots = new ArrayList<>();

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }

    public List<VolumeSnapshotInventory> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<VolumeSnapshotInventory> snapshots) {
        this.snapshots = snapshots;
    }
}
