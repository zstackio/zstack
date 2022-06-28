package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.devices.PciAddressConfig;
import org.zstack.header.vm.devices.VirtualDeviceInfo;
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
        to.setPciAddress(vidManager.getVmDevicePciAddress(to.getResourceUuid(), cmd.getVmInstanceUuid()));
    }

    @Override
    public void afterReceiveStartVmResponse(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmResponse rsp) {
        if (rsp.getVirtualDeviceInfoList() == null) {
            return;
        }

        // only update pci address, metadata is not mandatory in normal usage
        // check its usage when create snapshot or backup
        rsp.getVirtualDeviceInfoList().forEach(info -> {
            if (info.getResourceUuid() != null) {
                vidManager.createOrUpdateVmDeviceAddress(info, spec.getVmInventory().getUuid());
            }
        });

        if (rsp.getNicInfos() == null) {
            return;
        }

        rsp.getNicInfos().forEach(info -> {
            VmNicInventory nic = spec.getDestNics()
                    .stream()
                    .filter(vmNicInventory -> vmNicInventory.getMac().equals(info.getMacAddress()))
                    .findFirst()
                    .orElse(null);
            if (nic == null) {
                return;
            }

            vidManager.createOrUpdateVmDeviceAddress(new VirtualDeviceInfo(nic.getUuid(), info.getPciInfo()), spec.getVmInventory().getUuid());
        });

        if (!StringUtils.isEmpty(rsp.getMemBalloonInfo().getPciInfo().toString())) {
            vidManager.createOrUpdateVmDeviceAddress(new VirtualDeviceInfo(vidManager.MEMBALLOON_UUID, PciAddressConfig.fromString(rsp.getMemBalloonInfo().getPciInfo().toString())), spec.getVmInventory().getUuid());
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
