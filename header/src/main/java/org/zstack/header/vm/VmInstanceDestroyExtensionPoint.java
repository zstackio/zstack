package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public interface VmInstanceDestroyExtensionPoint {
    /**
     * Return true when a running VM must be stopped before the delete cascade starts.
     */
    default boolean needStopBeforeDestroy(VmInstanceInventory inv) {
        return false;
    }

    String preDestroyVm(VmInstanceInventory inv);

    void beforeDestroyVm(VmInstanceInventory inv);

    void afterDestroyVm(VmInstanceInventory inv);

    void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason);
}
