package org.zstack.header.volume;

import org.zstack.header.core.Completion;

/**
 * Created by frank on 11/24/2015.
 */
public interface RecoverDataVolumeExtensionPoint {
    void preRecoverDataVolume(VolumeInventory vol);

    void beforeRecoverDataVolume(VolumeInventory vol, Completion completion);

    void afterRecoverDataVolume(VolumeInventory vol);
}
