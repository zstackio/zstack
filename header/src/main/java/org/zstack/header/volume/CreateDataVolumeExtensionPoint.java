package org.zstack.header.volume;

/**
 * Created by mingjian.deng on 2017/9/20.
 */
public interface CreateDataVolumeExtensionPoint {
    void afterCreateVolume(VolumeInventory vm);
}
