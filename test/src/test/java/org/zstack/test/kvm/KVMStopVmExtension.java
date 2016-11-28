package org.zstack.test.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMException;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStopVmExtensionPoint;

public class KVMStopVmExtension implements KVMStopVmExtensionPoint {
    boolean beforecalled;
    boolean successCalled;
    boolean failCalled;

    @Override
    public void beforeStopVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException {
        beforecalled = true;
    }

    @Override
    public void stopVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        successCalled = true;
    }

    @Override
    public void stopVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {
        failCalled = true;
    }
}
