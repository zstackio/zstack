package org.zstack.test.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMException;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;

public class KVMStartVmExtension implements KVMStartVmExtensionPoint {
    public boolean beforeCalled = false;
    public boolean successCalled = false;
    public boolean failedCalled = false;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host,
                                   VmInstanceSpec order, StartVmCmd cmd) throws KVMException {
        beforeCalled = true;
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec order) {
        successCalled = true;
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host,
                                   VmInstanceSpec order, ErrorCode err) {
        failedCalled = true;
    }

}
