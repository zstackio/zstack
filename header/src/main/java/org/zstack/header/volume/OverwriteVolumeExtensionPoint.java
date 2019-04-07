package org.zstack.header.volume;

/**
 * Created by MaJin on 2019/4/2.
 */
public interface OverwriteVolumeExtensionPoint {
    void afterOverwriteVolume(VolumeInventory volume, VolumeInventory transientVolume);
}
