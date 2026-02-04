package org.zstack.kvm.tpm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;

public class KvmTpmExtensions implements KVMStartVmExtensionPoint {
    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        final VmDevicesSpec devicesSpec = spec.getDevicesSpec();
        if (devicesSpec == null || devicesSpec.getTpm() == null || !devicesSpec.getTpm().isEnable()) {
            return;
        }

        TpmTO tpm = new TpmTO();
        tpm.setKeyProviderUuid(devicesSpec.getTpm().getKeyProviderUuid());
        cmd.setTpm(tpm);
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
        // do-nothing
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        // do-nothing
    }
}
