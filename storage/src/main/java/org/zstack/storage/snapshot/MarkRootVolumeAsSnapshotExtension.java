package org.zstack.storage.snapshot;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by mingjian.deng on 2019/1/10.
 */
public interface MarkRootVolumeAsSnapshotExtension {
    void markRootVolumeAsSnapshot(final VolumeInventory vol, String accountUuid, final ReturnValueCompletion<String> completion);
    String getExtensionPrimaryStorageType();
}
