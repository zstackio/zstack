package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;

public interface KVMStartVmExtensionPoint {
	void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) throws KVMException;
	
	void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec);
	
	void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err);
}
