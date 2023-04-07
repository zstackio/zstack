package org.zstack.header.vm;

public class CreateTemplateFromVmRootVolumeSnapShotMsg extends CreateTemplateFromVmRootVolumeMsg {
    private String snapshotUuid;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
