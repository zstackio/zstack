package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeInventory;

/**
 * Created by kayo on 2018/8/16.
 */
public interface VolumeJustBeforeDeleteFromDbExtensionPoint {
    void volumeJustBeforeDeleteFromDb(VolumeInventory inv);
}
