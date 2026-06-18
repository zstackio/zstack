package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;

import java.util.List;

public interface PrimaryStorageNodeSvc {
    void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp);

    void deactivate(String installPath, String protocol, HostInventory h, Completion comp);

    void deactivate(String installPath, String protocol, ActiveVolumeClient client, Completion comp);

    void blacklist(String installPath, String protocol, HostInventory h);

    String getActivePath(BaseVolumeInfo v, HostInventory h, boolean shareable);
    BaseVolumeInfo getActiveVolumeInfo(String activePath, HostInventory h, boolean shareable);

    // TODO use HostInventory
    List<ActiveVolumeClient> getActiveClients(String installPath, String protocol);

    List<String> getActiveVolumesLocation(HostInventory h);

    // protocols lists every output protocol the host should be prepared for in one
    // pass; empty/null means a baseline deploy (CBD client registration only)
    void deployClient(HostInventory h, List<String> protocols, Completion comp);

    void activateHeartbeatVolume(HostInventory h, ReturnValueCompletion<HeartbeatVolumeTopology> comp);

    void deactivateHeartbeatVolume(HostInventory h, Completion comp);

    HeartbeatVolumeTopology getHeartbeatVolumeActiveInfo(HostInventory h);

    String getIdentity();
}
