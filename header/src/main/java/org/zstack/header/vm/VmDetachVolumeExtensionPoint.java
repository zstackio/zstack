package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by frank on 6/10/2015.
 */
public interface VmDetachVolumeExtensionPoint {
    void preDetachVolume(VmInstanceInventory vm, VolumeInventory volume);

    void beforeDetachVolume(VmInstanceInventory vm, VolumeInventory volume);

    void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume);

    void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode);
}
