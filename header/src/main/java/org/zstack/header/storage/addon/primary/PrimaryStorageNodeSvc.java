package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;

import java.util.List;

public interface PrimaryStorageNodeSvc {
    void getVolumeStats(VolumeInventory vol, ReturnValueCompletion<VolumeStats> comp);

    void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp);

    void deactivate(BaseVolumeInfo v, HostInventory h, Completion comp);

    ActiveVolumeTO getActiveResult(BaseVolumeInfo v, HostInventory h, boolean shareable);

    List<BaseVolumeInfo> getActiveVolumesInfo(List<String> activePaths, HostInventory h, boolean shareable);

    List<String> getActiveVolumesLocation(HostInventory h);

    void activateHeartbeatVolume(HostInventory h, ReturnValueCompletion<HeartbeatVolumeTO> comp);

    void deactivateHeartbeatVolume(HostInventory h, Completion comp);

    String getIdentity();
}
