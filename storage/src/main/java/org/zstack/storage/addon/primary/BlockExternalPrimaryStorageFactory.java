package org.zstack.storage.addon.primary;

import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.Map;

public interface BlockExternalPrimaryStorageFactory {
    String getType();

    BlockExternalPrimaryStorageBackend getBlockExternalPrimaryStorageBackend(PrimaryStorageVO vo);

    void activeIscsiVolume(String clientIqn, BaseVolumeInfo vol, boolean shareable);

    Map<String, String> getVolumeIscsiInfo(String volInstallPath, HostInventory host);

    String getVolumeLunId(String volInstallPath);
}
