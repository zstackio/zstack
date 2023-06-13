package org.zstack.header.storage.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.volume.VolumeInventory;

public interface DeleteDataVolumeExtensionPoint {
    void afterDeleteDataVolume(VolumeInventory selfInventory, ReturnValueCompletion<VolumeInventory> volumeInventoryReturnValueCompletion);
}
