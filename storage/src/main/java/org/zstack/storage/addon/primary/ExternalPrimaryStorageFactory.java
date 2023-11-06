package org.zstack.storage.addon.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.addon.backup.ExternalBackupStorageFactory;
import org.zstack.storage.snapshot.MarkRootVolumeAsSnapshotExtension;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class ExternalPrimaryStorageFactory implements PrimaryStorageFactory, Component, PSCapacityExtensionPoint,
        PreVmInstantiateResourceExtensionPoint, VmReleaseResourceExtensionPoint,
        VmAttachVolumeExtensionPoint, VmDetachVolumeExtensionPoint, BeforeTakeLiveSnapshotsOnVolumes,
        CreateTemplateFromVolumeSnapshotExtensionPoint, MarkRootVolumeAsSnapshotExtension {
    private static final CLogger logger = Utils.getLogger(ExternalBackupStorageFactory.class);
    public static PrimaryStorageType type = new PrimaryStorageType(PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE);

    protected static Map<String, PrimaryStorageControllerSvc> controllers = new HashMap<>();
    protected static Map<String, PrimaryStorageNodeSvc> nodes = new HashMap<>();

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;


    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(ExternalPrimaryStorageSvcBuilder.class, ExternalPrimaryStorageSvcBuilder::getIdentity);
        List<ExternalPrimaryStorageVO> extPs = dbf.listAll(ExternalPrimaryStorageVO.class);
        for (ExternalPrimaryStorageVO vo : extPs) {
            saveController(vo);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv) {
        PrimaryStorageControllerSvc controller = controllers.get(psInv.getUuid());
        if (controller == null) {
            return psInv.getUrl();
        }

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setDryRun(true);
        aspec.setSize(msg.getSize());
        aspec.setRequiredUrl(msg.getRequiredInstallUri());
        return controller.allocateSpace(aspec);
    }

    @Override
    public long reserveCapacity(AllocatePrimaryStorageSpaceMsg msg, String allocatedInstallUrl, long size, String psUuid) {
        PrimaryStorageControllerSvc controller = controllers.get(psUuid);
        if (controller == null) {
            return size;
        }

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setDryRun(false);
        aspec.setSize(msg.getSize());
        aspec.setRequiredUrl(msg.getRequiredInstallUri());
        controller.allocateSpace(aspec);
        return size;
    }

    @Override
    public void releaseCapacity(String allocatedInstallUrl, long size, String psUuid) {

    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        APIAddExternalPrimaryStorageMsg amsg = (APIAddExternalPrimaryStorageMsg) msg;
        String identity = amsg.getIdentity();
        ExternalPrimaryStorageSvcBuilder builder = getSvcBuilder(identity);
        if (builder == null) {
            throw new OperationFailureException(
                    Platform.operr("No primary storage plugin registered with identity: %s", identity)
            );
        }

        final ExternalPrimaryStorageVO lvo = new ExternalPrimaryStorageVO(vo);
        lvo.setIdentity(identity);
        lvo.setDefaultProtocol(amsg.getDefaultOutputProtocol());
        lvo.setConfig(amsg.getConfig());
        lvo.setMountPath(identity);
        dbf.persist(lvo);

        saveController(lvo);
        return lvo.toInventory();
    }

    private void saveController(ExternalPrimaryStorageVO extVO) {
        ExternalPrimaryStorageSvcBuilder builder = getSvcBuilder(extVO.getIdentity());
        PrimaryStorageControllerSvc controller = builder.buildControllerSvc(extVO);
        controllers.put(extVO.getUuid(), controller);
        if (controller instanceof PrimaryStorageNodeSvc) {
            nodes.put(extVO.getUuid(), (PrimaryStorageNodeSvc) controller);
        } else {
            nodes.put(extVO.getUuid(), builder.buildNodeSvc(extVO));
        }
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new ExternalPrimaryStorage(vo, controllers.get(vo.getUuid()), nodes.get(vo.getUuid()));
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return ExternalPrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, ExternalPrimaryStorageVO.class));
    }

    public PrimaryStorageControllerSvc getControllerSvc(String primaryStorageUuid) {
        return controllers.get(primaryStorageUuid);
    }

    public PrimaryStorageNodeSvc getNodeSvc(String primaryStorageUuid) {
        return nodes.get(primaryStorageUuid);
    }

    private PrimaryStorageNodeSvc getNodeSvcByVolume(VolumeInventory volumeInventory) {
        if (volumeInventory.getPrimaryStorageUuid() == null) {
            return null;
        }

        String identity = volumeInventory.getInstallPath().split("://")[0];
        if (!support(identity)) {
            return null;
        }

        return getNodeSvc(volumeInventory.getPrimaryStorageUuid());
    }

    public boolean support(String identity) {
        return getSvcBuilder(identity) != null;
    }

    private ExternalPrimaryStorageSvcBuilder getSvcBuilder(String identity) {
        return pluginRgty.getExtensionFromMap(identity, ExternalPrimaryStorageSvcBuilder.class);
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
            PrimaryStorageNodeSvc svc = getNodeSvc(vol.getPrimaryStorageUuid());
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
            PrimaryStorageNodeSvc svc = getNodeSvc(vol.getPrimaryStorageUuid());
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
        vols.add(BaseVolumeInfo.valueOf(spec.getDestRootVolume()));
        spec.getDestDataVolumes().forEach(vol -> vols.add(BaseVolumeInfo.valueOf(vol)));

        spec.getCdRomSpecs().forEach(cdRomSpec -> {
            if (cdRomSpec.getInstallPath() != null && cdRomSpec.getProtocol() != null) {
                BaseVolumeInfo info = new BaseVolumeInfo();
                info.setInstallPath(cdRomSpec.getInstallPath());
                info.setProtocol(cdRomSpec.getProtocol());
                vols.add(info);
            }
        });

        vols.removeIf(info -> {
            if (info.getInstallPath() == null) {
                return true;
            }
            String identity = info.getInstallPath().split("://")[0];
            return !support(identity);
        });

        return vols;
    }

    private void activeVolumeIfNeed(VmInstanceInventory vm, VolumeInventory volume) {
        PrimaryStorageNodeSvc svc = getNodeSvcByVolume(volume);
        if (svc == null) {
            return;
        }

        if (vm.getHostUuid() == null || VmInstanceState.Stopped.toString().equals(vm.getState())) {
            return;
        }

        HostInventory host = HostInventory.valueOf(dbf.findByUuid(vm.getHostUuid(), HostVO.class));
        // TODO change interface
        svc.activate(BaseVolumeInfo.valueOf(volume), host, volume.isShareable(), new NopeReturnValueCompletion());
    }

    private void deactivateVolumeIfNeed(VmInstanceInventory vm, VolumeInventory volume) {
        PrimaryStorageNodeSvc svc = getNodeSvcByVolume(volume);
        if (svc == null) {
            return;
        }

        if (vm.getHostUuid() == null) {
            return;
        }

        HostInventory host = HostInventory.valueOf(dbf.findByUuid(vm.getHostUuid(), HostVO.class));
        // TODO change interface
        svc.deactivate(BaseVolumeInfo.valueOf(volume), host, new NopeCompletion());
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        activeVolumeIfNeed(vm, volume);
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {}

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {
        activeVolumeIfNeed(vm, volume);
    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {
        deactivateVolumeIfNeed(vm, volume);
    }

    @Override
    public void preDetachVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    @Override
    public void beforeDetachVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        deactivateVolumeIfNeed(vm, volume);
        completion.success();
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {}

    @Override
    public void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmMsg otmsg, Map flowData, Completion completion) {
        List<CreateVolumesSnapshotsJobStruct> storageSnapshots = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct struct : msg.getVolumeSnapshotJobs()) {
            PrimaryStorageControllerSvc svc = getControllerSvc(struct.getPrimaryStorageUuid());
            if (svc != null && !svc.reportCapabilities().getSnapshotCapability().isSupportCreateOnHypervisor()) {
                storageSnapshots.add(struct);
                otmsg.getSnapshotJobs().removeIf(job -> job.getVolumeUuid().equals(struct.getVolumeUuid()));
            }
        }

        if (storageSnapshots.isEmpty()) {
            completion.success();
            return;
        }

        if (otmsg.getSnapshotJobs().isEmpty()) {
            flowData.put(VolumeSnapshotConstant.NEED_BLOCK_STREAM_ON_HYPERVISOR, false);
            flowData.put(VolumeSnapshotConstant.NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR, false);
        } else if (msg.getConsistentType() != ConsistentType.None) {
            completion.fail(operr("not support take volumes snapshots " +
                    "on multiple ps when including storage snapshot"));
            return;
        }

        logger.info(String.format("take snapshots for volumes[%s] on %s",
                msg.getLockedVolumeUuids(), getClass().getCanonicalName()));

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(storageSnapshots).all((struct, whileCompletion) -> {
            VolumeSnapshotVO vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, struct.getResourceUuid()).find();
            if (vo.getStatus().equals(VolumeSnapshotStatus.Ready)) {
                logger.warn(String.format("snapshot %s on volume %s is ready, no need to create again!",
                        vo.getUuid(), vo.getVolumeUuid()));
                whileCompletion.done();
                return;
            }
            TakeSnapshotMsg tmsg = new TakeSnapshotMsg();
            tmsg.setPrimaryStorageUuid(struct.getPrimaryStorageUuid());
            tmsg.setStruct(struct.getVolumeSnapshotStruct());
            bus.makeTargetServiceIdByResourceUuid(tmsg, PrimaryStorageConstant.SERVICE_ID, tmsg.getPrimaryStorageUuid());
            bus.send(tmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        whileCompletion.done();
                        return;
                    }
                    TakeSnapshotReply treply = reply.castReply();
                    if (!treply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        whileCompletion.done();
                        return;
                    }

                    vo.setPrimaryStorageInstallPath(treply.getInventory().getPrimaryStorageInstallPath());
                    vo.setSize(treply.getInventory().getSize());
                    vo.setPrimaryStorageUuid(treply.getInventory().getPrimaryStorageUuid());
                    vo.setType(treply.getInventory().getType());
                    vo.setFormat(treply.getInventory().getFormat());
                    vo.setStatus(VolumeSnapshotStatus.Ready);
                    dbf.update(vo);

                    struct.getVolumeSnapshotStruct().setCurrent(treply.getInventory());
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();


        template.setCreateTemporaryTemplate(new NopeFlow());

        template.setUploadToBackupStorage(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);

                CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg();
                cmsg.setSnapshotUuid(paramIn.getSnapshot().getUuid());
                cmsg.setImageInventory(paramIn.getImage());
                cmsg.setBackupStorageUuid(paramIn.getBackupStorageUuid());

                VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(paramIn.getSnapshot().getVolumeUuid(), VolumeVO.class));
                cmsg.setVolumeInventory(vol);
                bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateTemplateFromVolumeOnPrimaryStorageReply r = reply.castReply();
                        out.setBackupStorageInstallPath(r.getTemplateBackupStorageInstallPath());
                        out.setActualSize(r.getActualSize());
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (out.getBackupStorageInstallPath() != null) {
                    DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                    msg.setInstallPath(out.getBackupStorageInstallPath());
                    msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                    bus.send(msg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            trigger.rollback();
                        }
                    });
                }
            }
        });

        template.setDeleteTemporaryTemplate(new NopeFlow());
        return template;
    }

    @Override
    public List<Flow> markRootVolumeAsSnapshot(VolumeInventory vol, VolumeSnapshotVO vo, String accountUuid) {
        PrimaryStorageControllerSvc svc = getControllerSvc(vol.getPrimaryStorageUuid());
        if (svc == null) {
            return null;
        }

        VolumeSnapshotCapability snapCap = svc.reportCapabilities().getSnapshotCapability();
        if (snapCap.getArrangementType() == VolumeSnapshotCapability.VolumeSnapshotArrangementType.CHAIN) {
            return null;
        }

        List<Flow> flows = new ArrayList<>();
        flows.add(new NoRollbackFlow() {
            String __name__ = "create-snapshot-before-reimage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                cmsg.setAccountUuid(accountUuid);
                cmsg.setVolumeUuid(vol.getUuid());
                cmsg.setName(vol.getName());
                cmsg.setDescription(vol.getDescription());
                cmsg.setDescription(vol.getDescription());

                bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateVolumeSnapshotReply r = (CreateVolumeSnapshotReply)reply;
                        vo.setUuid(r.getInventory().getUuid());
                        if (snapCap.isSupportCreateOnHypervisor()) {
                            vo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
                        } else {
                            vo.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                        }
                        trigger.next();
                    }
                });
            }
        });
        return flows;
    }

    @Override
    public String getExtensionPrimaryStorageType() {
        return PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE;
    }
}
