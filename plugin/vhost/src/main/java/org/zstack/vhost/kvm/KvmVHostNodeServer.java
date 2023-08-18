package org.zstack.vhost.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.storage.addon.primary.ActiveVolumeTO;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.kvm.*;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KvmVHostNodeServer implements Component, KVMStartVmExtensionPoint, PreVmInstantiateResourceExtensionPoint,
        VmReleaseResourceExtensionPoint, KVMConvertVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, KVMAttachVolumeExtensionPoint {
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

        /* todo CD ROM, share mem
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


    private VolumeTO convertVolumeIfNeeded(VolumeInventory volumeInventory, VolumeTO volumeTO) {
        String identity = volumeInventory.getInstallPath().split("://")[0];
        if (!extPsFactory.support(identity)) {
            return volumeTO;
        }

        PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(volumeInventory.getPrimaryStorageUuid());
        if (nodeSvc == null) {
            return volumeTO;
        }

        ActiveVolumeTO vol = nodeSvc.getActiveResult(BaseVolumeInfo.valueOf(volumeInventory), false);
        volumeTO.setInstallPath(vol.getInstallPath());
        return volumeTO;
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation().equals(VmInstanceConstant.VmOperation.ChangeImage)) {
            completion.success();
            return;
        }

        List<BaseVolumeInfo> vols = getManagerVolume(spec);

        if (vols.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(vols).each((vol, compl) -> {
            PrimaryStorageNodeSvc svc = extPsFactory.getNodeSvc(vol.getPrimaryStorageUuid());
            svc.activate(vol, spec.getDestHost(), vol.isShareable(), new ReturnValueCompletion<ActiveVolumeTO>(compl) {
                @Override
                public void success(ActiveVolumeTO v) {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    // todo rollback
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation().equals(VmInstanceConstant.VmOperation.ChangeImage)) {
            completion.success();
            return;
        }

        List<BaseVolumeInfo> vols = getManagerVolume(spec);
        if (vols.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(vols).each((vol, compl) -> {
            PrimaryStorageNodeSvc svc = extPsFactory.getNodeSvc(vol.getPrimaryStorageUuid());
            svc.deactivate(vol, spec.getDestHost(), new Completion(compl) {
                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    // todo rollback
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        preReleaseVmResource(spec, completion);
    }

    private List<BaseVolumeInfo> getManagerVolume(VmInstanceSpec spec) {
        List<BaseVolumeInfo> vols = new ArrayList<>();
        if (VolumeProtocol.VHost.toString().equals(spec.getDestRootVolume().getProtocol())) {
            vols.add(BaseVolumeInfo.valueOf(spec.getDestRootVolume()));
        }

        spec.getDestDataVolumes().forEach(vol -> {
            if (VolumeProtocol.VHost.toString().equals(vol.getProtocol())) {
                vols.add(BaseVolumeInfo.valueOf(vol));
            }
        });

        spec.getCdRomSpecs().forEach(cdRomSpec -> {
            if (VolumeProtocol.VHost.toString().equals(cdRomSpec.getProtocol())) {
                BaseVolumeInfo info = new BaseVolumeInfo();
                info.setInstallPath(cdRomSpec.getInstallPath());
                info.setProtocol(VolumeProtocol.valueOf(cdRomSpec.getProtocol()));
                vols.add(info);
            }
        });

        vols.removeIf(info -> {
            String identity = info.getInstallPath().split("://")[0];
            return !extPsFactory.support(identity);
        });

        return vols;
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
