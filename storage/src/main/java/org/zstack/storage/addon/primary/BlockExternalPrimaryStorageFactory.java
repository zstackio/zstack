package org.zstack.storage.addon.primary;

import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.Map;

public interface BlockExternalPrimaryStorageFactory {
    String getType();

    BlockExternalPrimaryStorageBackend getBlockExternalPrimaryStorageBackend(PrimaryStorageVO vo);
}
