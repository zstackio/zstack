package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;

public interface PrimaryStorageNodeSvc {
    void getVolumeStats(VolumeInventory vol, ReturnValueCompletion<VolumeStats> comp);

    void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp);

    ActiveVolumeTO getActiveResult(BaseVolumeInfo v, HostInventory h, boolean shareable);

    void deactivate(BaseVolumeInfo v, HostInventory h, Completion comp);

    String getIdentity();
}
