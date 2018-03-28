package org.zstack.header.volume;

/**
 * Created by mingjian.deng on 2017/9/20.
 */
public interface CreateDataVolumeExtensionPoint {
    void beforeCreateVolume(VolumeInventory vm);
    void afterCreateVolume(VolumeInventory vm);
}
