package org.zstack.storage.volume;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by yaoning.li on 2021/2/19.
 */
public interface VolumeCascadeExtensionPoint {
    ErrorCode preDestroyVm(VolumeInventory inv);
}
