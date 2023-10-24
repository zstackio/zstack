package org.zstack.vhost.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.addon.primary.ActiveVolumeTO;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.*;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KvmVHostNodeServer implements Component, KVMStartVmExtensionPoint,
        KVMConvertVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, KVMAttachVolumeExtensionPoint {
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    private PluginRegistry pluginRgty;


    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        cmd.setRootVolume(convertVolumeIfNeeded(spec.getDestRootVolume(), cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dtos.add(convertVolumeIfNeeded(vol, to));
                    break;
                }
            }
        }

        cmd.setDataVolumes(dtos);

        /* todo CD ROM
        for (KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
            if (cdRomTO.isEmpty()) {
                continue;
            }
            cdRomTO.setPath(convertVolumeIfNeeded(cdRomTO.getPath()));
        }
        */

        cmd.setUseHugePage(true);
        cmd.setMemAccess("shared");
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }


    private PrimaryStorageNodeSvc getNodeService(VolumeInventory volumeInventory) {
        String identity = volumeInventory.getInstallPath().split("://")[0];
        if (!extPsFactory.support(identity)) {
            return null;
        }

        return extPsFactory.getNodeSvc(volumeInventory.getPrimaryStorageUuid());
    }

    private VolumeTO convertVolumeIfNeeded(VolumeInventory volumeInventory, VolumeTO volumeTO) {
        PrimaryStorageNodeSvc nodeSvc = getNodeService(volumeInventory);
        if (nodeSvc == null) {
            return volumeTO;
        }

        ActiveVolumeTO vol = nodeSvc.getActiveResult(BaseVolumeInfo.valueOf(volumeInventory), false);
        volumeTO.setInstallPath(vol.getInstallPath());
        return volumeTO;
    }


    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeIfNeeded(inventory, to);
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        cmd.setVolume(convertVolumeIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {}
    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {}
}
