package org.zstack.header.volume;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;

/**
 * Created by xing5 on 2016/5/3.
 */
public interface VolumeBeforeExpungeExtensionPoint {
    void volumePreExpunge(VolumeInventory volume);
    void volumeBeforeExpunge(VolumeInventory volume, NoErrorCompletion completion);

    default boolean skipExpungeVolume(VolumeInventory volume) {
        return false;
    }
}
