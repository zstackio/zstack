package org.zstack.kvm;

import org.zstack.header.vm.VmInstanceInventory;

/**
 * @Author: DaoDao
 * @Date: 2022/7/26
 */
public interface KVMSyncVmDeviceInfoExtensionPoint {
    default void afterReceiveVmDeviceInfoResponse(VmInstanceInventory vm, KVMAgentCommands.VmDevicesInfoResponse rsp) {
    }
}
