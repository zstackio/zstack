package org.zstack.storage.addon.primary;

import org.zstack.header.storage.primary.DeleteSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.TakeSnapshotMsg;
import org.zstack.header.volume.block.GetAccessPathMsg;

/**
 * do not match external primary storage design.
 */
@Deprecated
public interface BlockExternalPrimaryStorageBackend {
    String getType();

    void handle(InstantiateVolumeOnPrimaryStorageMsg msg);

    void handle(DeleteVolumeOnPrimaryStorageMsg msg);

    void handle(TakeSnapshotMsg msg);

    void handle(final DeleteSnapshotOnPrimaryStorageMsg msg);

    void handle(GetAccessPathMsg msg);
}
