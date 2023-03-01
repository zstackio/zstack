package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmAfterExpungeExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.*;

import java.util.Optional;

import static org.zstack.kvm.KVMAgentCommands.*;

/**
 * Created by Wenhao.Zhang on 23/02/27
 */
public class KvmHypervisorInfoExtensions implements
        KVMSyncVmDeviceInfoExtensionPoint,
        KVMRebootVmExtensionPoint,
        KVMDestroyVmExtensionPoint,
        KVMStopVmExtensionPoint,
        VmAfterExpungeExtensionPoint
{
    @Autowired
    private KvmHypervisorInfoManager manager;

    @Override
    public void afterReceiveVmDeviceInfoResponse(VmInstanceInventory vm, VmDevicesInfoResponse rsp) {
        Optional.ofNullable(rsp.getVirtualizerInfo()).ifPresent(manager::save);
    }

    @Override
    public void beforeRebootVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException {}
    @Override
    public void rebootVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {}

    @Override
    public void rebootVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm, RebootVmResponse rsp) {
        Optional.ofNullable(rsp.getVirtualizerInfo()).ifPresent(manager::save);
    }

    @Override
    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, DestroyVmCmd cmd)
            throws KVMException {}

    @Override
    public void beforeDirectlyDestroyVmOnKvm(DestroyVmCmd cmd) {}

    @Override
    public void destroyVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        manager.clean(vm.getUuid());
    }

    @Override
    public void destroyVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {}

    @Override
    public void beforeStopVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, StopVmCmd cmd) throws KVMException {}

    @Override
    public void stopVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        manager.clean(vm.getUuid());
    }

    @Override
    public void stopVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {}

    @Override
    public void vmAfterExpunge(VmInstanceInventory vm) {
        manager.clean(vm.getUuid());
    }
}
