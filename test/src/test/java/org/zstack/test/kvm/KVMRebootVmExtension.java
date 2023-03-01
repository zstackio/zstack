package org.zstack.test.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMException;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMRebootVmExtensionPoint;

public class KVMRebootVmExtension implements KVMRebootVmExtensionPoint {
    boolean beforeCalled;
    boolean successCalled;
    boolean failCalled;

    @Override
    public void beforeRebootVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException {
        beforeCalled = true;
    }

    @Override
    public void rebootVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.RebootVmResponse ret) {
        successCalled = true;
    }

    @Override
    public void rebootVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {
        failCalled = true;
    }

}
