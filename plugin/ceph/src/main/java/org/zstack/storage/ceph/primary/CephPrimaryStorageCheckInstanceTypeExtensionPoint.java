package org.zstack.storage.ceph.primary;

import org.zstack.header.volume.VolumeVO;

public interface CephPrimaryStorageCheckInstanceTypeExtensionPoint {
    Boolean isSupportCloneByThirdParty(String uuid);

    void convertToBlockVolume(VolumeVO vo);
}
