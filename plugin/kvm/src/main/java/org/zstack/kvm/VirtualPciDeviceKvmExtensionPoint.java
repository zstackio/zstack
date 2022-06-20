package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.vm.devices.VmInstanceDeviceManager;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;

public class VirtualPciDeviceKvmExtensionPoint implements KVMStartVmExtensionPoint {
    @Autowired
    private VmInstanceDeviceManager vidManager;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (cmd.getRootVolume() != null) {
            setPciAddress(cmd.getRootVolume(), cmd);
        }

        if (cmd.getDataVolumes() != null) {
            cmd.getDataVolumes().forEach(to -> setPciAddress(to, cmd));
        }

        if (cmd.getNics() != null) {
            cmd.getNics().forEach(to -> setPciAddress(to, cmd));
        }

        if (cmd.getCdRoms() != null) {
            cmd.getCdRoms().forEach(to -> setPciAddress(to, cmd));
        }
    }

    private void setPciAddress(BaseVirtualPciDeviceTO to, KVMAgentCommands.StartVmCmd cmd) {
        to.setResourceUuid(cmd.getRootVolume().getVolumeUuid());
        cmd.getRootVolume().setPciAddress(vidManager.getVmDevicePciAddress(to.getResourceUuid(), cmd.getVmInstanceUuid()));
    }

    @Override
    public void afterReceiveStartVmResponse(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmResponse rsp) {
        if (rsp.getVirtualDeviceInfoList() == null) {
            return;
        }

        // only update pci address, metadata is not mandatory in normal usage
        // check its usage when create snapshot or backup
        rsp.getVirtualDeviceInfoList().forEach(info -> vidManager.createOrUpdateVmDeviceAddress(info, spec.getVmInventory().getUuid()));
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
