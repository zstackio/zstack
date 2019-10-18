package org.zstack.header.volume;

import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/5/3.
 */
public interface VolumeBeforeExpungeExtensionPoint {
    void volumeBeforeExpunge(VolumeInventory volume, Completion completion);
}
