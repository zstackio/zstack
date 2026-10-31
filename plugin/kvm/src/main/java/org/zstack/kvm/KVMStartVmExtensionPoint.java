package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;

public interface KVMStartVmExtensionPoint {
	default void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) {}

	default void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd, Completion completion) {
		beforeStartVmOnKvm(host, spec, cmd);
		completion.success();
	}
	
	void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec);

	default void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {}

	default void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err, Completion completion) {
		startVmOnKvmFailed(host, spec, err);
		completion.success();
	}
}
