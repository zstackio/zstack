package org.zstack.header.storage.snapshot;

import java.util.List;

public class VolumeSnapshotDeletionStructs {
    private List<VolumeSnapshotInventory> snapshotInventories;
    private String direction;
    private String scope;
    private String snapshotGroupUuid;

    public VolumeSnapshotDeletionStructs(List<VolumeSnapshotInventory> snapshotInventories, String direction, String scope) {
        this.snapshotInventories = snapshotInventories;
        this.direction = direction;
        this.scope = scope;
    }

    public String getSnapshotGroupUuid() {
        return snapshotGroupUuid;
    }

    public void setSnapshotGroupUuid(String snapshotGroupUuid) {
        this.snapshotGroupUuid = snapshotGroupUuid;
    }

    public List<VolumeSnapshotInventory> getSnapshotInventories() {
        return snapshotInventories;
    }

    public void setSnapshotInventories(List<VolumeSnapshotInventory> snapshotInventories) {
        this.snapshotInventories = snapshotInventories;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
