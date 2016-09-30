package org.zstack.header.storage.snapshot;

/**
 */
public interface VolumeSnapshotMessage {
    String getSnapshotUuid();

    String getVolumeUuid();

    void setVolumeUuid(String treeUuid);

    void setTreeUuid(String treeUuid);

    String getTreeUuid();
}
