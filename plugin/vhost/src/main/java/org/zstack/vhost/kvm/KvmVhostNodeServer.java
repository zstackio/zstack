package org.zstack.vhost.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeProtocolCapability;
import org.zstack.kvm.*;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public class KvmVhostNodeServer implements Component, KVMStartVmExtensionPoint,
        KVMConvertVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, KVMAttachVolumeExtensionPoint,
        KvmVmActiveVolumeSyncExtensionPoint {
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    private PluginRegistry pluginRgty;

    private static final VolumeProtocolCapability capability = VolumeProtocolCapability
            .register(VolumeProtocol.Vhost.name(), KVMConstant.KVM_HYPERVISOR_TYPE);

    static  {
        capability.setSupportQosOnHypervisor(false);
        capability.setSupportResizeOnHypervisor(false);
        capability.setSupportReadonly(false);
    }


    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        cmd.setRootVolume(convertVolumeIfNeeded(spec.getDestRootVolume(), host, cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dtos.add(convertVolumeIfNeeded(vol, host, to));
                    break;
                }
            }
        }

        cmd.setDataVolumes(dtos);
        cmd.setUseHugePage(true);
        cmd.setMemAccess("shared");

        // vhostuser disk not support readonly mode, so no iso.
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

    private VolumeTO convertVolumeIfNeeded(VolumeInventory volumeInventory, HostInventory host, VolumeTO volumeTO) {
        if (!VolumeProtocol.Vhost.name().equals(volumeInventory.getProtocol())) {
            return volumeTO;
        }

        if (!volumeTO.isUseVirtio()) {
            throw new OperationFailureException(
                    argerr("vhostuser disk only support virtio mode, check image platform has virtio driver or not"));
        }

        if (volumeTO.isUseVirtioSCSI()) {
            throw new OperationFailureException(
                    argerr("vhostuser disk not support virtio-scsi mode, please turn off virtio-scsi mode"));
        }

        PrimaryStorageNodeSvc nodeSvc = getNodeService(volumeInventory);
        if (nodeSvc == null) {
            return volumeTO;
        }

        String path = nodeSvc.getActivePath(BaseVolumeInfo.valueOf(volumeInventory), host,false);
        volumeTO.setInstallPath(path);
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
        return convertVolumeIfNeeded(inventory, host, to);
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {}
    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {}

    @Override
    public List<String> getStoragePathsForVolumeSync(HostInventory host, PrimaryStorageInventory attachedPs) {
        if (!PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE.equals(attachedPs.getType())) {
            return null;
        }

        PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(attachedPs.getUuid());
        if (nodeSvc == null) {
            return null;
        }

        return nodeSvc.getActiveVolumesLocation(host);
    }

    @Override
    public void handleInactiveVolume(HostInventory host, Map<PrimaryStorageInventory, List<String>> inactiveVolumePaths, Completion completion) {
        if (inactiveVolumePaths.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(inactiveVolumePaths.entrySet()).all((entry, compl) -> {
            PrimaryStorageInventory ps = entry.getKey();
            List<String> paths = entry.getValue();

            PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(ps.getUuid());
            if (nodeSvc == null) {
                compl.done();
                return;
            }

            List<BaseVolumeInfo> infos = paths.stream()
                    .map(path -> nodeSvc.getActiveVolumeInfo(path, host, false))
                    .collect(Collectors.toList());
            if (infos.isEmpty()) {
                compl.done();
                return;
            }

            new While<>(infos).each((info, c) -> {
                nodeSvc.deactivate(info.getInstallPath(), info.getProtocol(), host, new Completion(c) {
                    @Override
                    public void success() {
                        c.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        c.addError(errorCode);
                        c.done();
                    }
                });
            }).run(new WhileDoneCompletion(compl) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (!errorCodeList.getCauses().isEmpty()) {
                        compl.addError(errorCodeList.getCauses().get(0));
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }
}
