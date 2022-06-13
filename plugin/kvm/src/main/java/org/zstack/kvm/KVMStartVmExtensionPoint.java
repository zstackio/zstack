package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;

public interface KVMStartVmExtensionPoint {
	void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd);
	
	void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec);

	default void afterReceiveStartVmResponse(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmResponse rsp) {

	}
	
	void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err);
}
