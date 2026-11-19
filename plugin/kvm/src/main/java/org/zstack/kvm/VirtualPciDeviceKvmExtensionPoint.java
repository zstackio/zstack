package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.header.vm.devices.VirtualDeviceInfo;
import org.zstack.header.vm.devices.VmInstanceDeviceManager;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VirtualPciDeviceKvmExtensionPoint implements KVMStartVmExtensionPoint, KVMSyncVmDeviceInfoExtensionPoint {
    @Autowired
    private VmInstanceDeviceManager vidManager;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (cmd.getRootVolume() != null) {
            setDeviceAddress(cmd.getRootVolume(), cmd);
        }

        if (cmd.getDataVolumes() != null) {
            cmd.getDataVolumes().forEach(to -> setDeviceAddress(to, cmd));
        }

        if (cmd.getNics() != null) {
            cmd.getNics().forEach(to -> setDeviceAddress(to, cmd));
        }

        if (cmd.getCdRoms() != null) {
            cmd.getCdRoms().forEach(to -> setDeviceAddress(to, cmd));
        }
    }

    private void setDeviceAddress(BaseVirtualDeviceTO to, KVMAgentCommands.StartVmCmd cmd) {
        to.setDeviceAddress(vidManager.getVmDeviceAddress(to.getResourceUuid(), cmd.getVmInstanceUuid()));
    }

    @Override
    public void afterReceiveVmDeviceInfoResponse(VmInstanceInventory vm, KVMAgentCommands.VmDevicesInfoResponse rsp, VmInstanceSpec spec) {
        if (rsp.getVirtualDeviceInfoList() == null) {
            return;
        }

        String vmUuid = spec != null ? spec.getVmInventory().getUuid() : vm.getUuid();
        // only update pci address, metadata is not mandatory in normal usage
        // check its usage when create snapshot or backup
        rsp.getVirtualDeviceInfoList().forEach(info -> {
            if (info.isValid()) {
                vidManager.createOrUpdateVmDeviceAddress(info, vmUuid);
            }
        });

        if (rsp.getNicInfos() == null) {
            return;
        }

        List<VmNicInventory> orderedNics = (spec != null ? spec.getDestNics() : vm.getVmNics())
                .stream()
                .sorted(Comparator.comparingInt(VmNicInventory::getDeviceId))
                .collect(Collectors.toList());
        Set<String> matchedNicUuids = new HashSet<>();

        rsp.getNicInfos().forEach(info -> {
            VmNicInventory nic = null;
            if (!StringUtils.isEmpty(info.getNicInternalName())) {
                nic = orderedNics.stream()
                        .filter(n -> info.getNicInternalName().equals(n.getInternalName()))
                        .findFirst()
                        .orElse(null);
            }
            if (nic == null) {
                nic = orderedNics.stream()
                        .filter(n -> !matchedNicUuids.contains(n.getUuid())
                                && StringUtils.equalsIgnoreCase(n.getMac(), info.getMacAddress()))
                        .findFirst()
                        .orElse(null);
            }
            if (nic == null) {
                return;
            }

            matchedNicUuids.add(nic.getUuid());
            vidManager.createOrUpdateVmDeviceAddress(new VirtualDeviceInfo(nic.getUuid(), info.getDeviceAddress()), vmUuid);
        });

        if (!StringUtils.isEmpty(rsp.getMemBalloonInfo().getDeviceAddress().toString())) {
            vidManager.createOrUpdateVmDeviceAddress(new VirtualDeviceInfo(vidManager.MEM_BALLOON_UUID,
                    DeviceAddress.fromString(rsp.getMemBalloonInfo().getDeviceAddress().toString())), vmUuid);
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
