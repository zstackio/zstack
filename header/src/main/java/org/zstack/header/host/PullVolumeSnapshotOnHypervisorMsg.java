package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.List;

public class PullVolumeSnapshotOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    VolumeInventory volume;
    private String srcSnapshotParentPath;
    private VolumeSnapshotInventory srcSnapshot;
    private VolumeSnapshotInventory dstSnapshot;
    private String hostUuid;
    private List<String> chainInstallPathInDb = new ArrayList<>();

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public String getSrcSnapshotParentPath() {
        return srcSnapshotParentPath;
    }

    public void setSrcSnapshotParentPath(String srcSnapshotParentPath) {
        this.srcSnapshotParentPath = srcSnapshotParentPath;
    }

    public VolumeSnapshotInventory getSrcSnapshot() {
        return srcSnapshot;
    }

    public void setSrcSnapshot(VolumeSnapshotInventory srcSnapshot) {
        this.srcSnapshot = srcSnapshot;
    }

    public VolumeSnapshotInventory getDstSnapshot() {
        return dstSnapshot;
    }

    public void setDstSnapshot(VolumeSnapshotInventory dstSnapshot) {
        this.dstSnapshot = dstSnapshot;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getChainInstallPathInDb() {
        return chainInstallPathInDb;
    }

    public void setChainInstallPathInDb(List<String> chainInstallPathInDb) {
        this.chainInstallPathInDb = chainInstallPathInDb;
    }
}
