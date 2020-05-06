package org.zstack.test.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMDestroyVmExtensionPoint;
import org.zstack.kvm.KVMException;
import org.zstack.kvm.KVMHostInventory;

public class KVMDestroyVmExtension implements KVMDestroyVmExtensionPoint {
    boolean beforeCalled;
    boolean successCalled;
    boolean failCalled;

    @Override
    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.DestroyVmCmd cmd) throws KVMException {
        beforeCalled = true;
    }

    @Override
    public void beforeDirectlyDestroyVmOnKvm(KVMAgentCommands.DestroyVmCmd cmd) {

    }

    @Override
    public void destroyVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        successCalled = true;
    }

    @Override
    public void destroyVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {
        failCalled = true;
    }
}
