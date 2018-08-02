package org.zstack.storage.volume;

import org.zstack.header.vm.ChangeVmImageExtensionPoint;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by mingjian.deng on 2018/8/2.
 */
public interface ChangeVolumeInstallPathExtensionPoint extends ChangeVmImageExtensionPoint {
    void afterChangeVmRootVolumeInstallPath(String oldVolumeUuid, VolumeInventory newVol);
}
