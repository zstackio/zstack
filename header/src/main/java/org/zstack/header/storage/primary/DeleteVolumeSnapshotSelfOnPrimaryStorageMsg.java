package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:47 2023/8/21
 */
public class DeleteVolumeSnapshotSelfOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String vmUuid;
    private VolumeInventory volume;
    private VolumeSnapshotInventory snapshot;
    private String srcPath;
    private String dstPath;
    private String primaryStorageUuid;
    private List<String> aliveChainInstallPathInDb;

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getSrcPath() {
        return srcPath;
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public String getDstPath() {
        return dstPath;
    }

    public void setDstPath(String dstPath) {
        this.dstPath = dstPath;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public List<String> getAliveChainInstallPathInDb() {
        return aliveChainInstallPathInDb;
    }

    public void setAliveChainInstallPathInDb(List<String> aliveChainInstallPathInDb) {
        this.aliveChainInstallPathInDb = aliveChainInstallPathInDb;
    }
}
