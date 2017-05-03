package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmExpungeRootVolumeValidator;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.path.PathUtil;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class NfsPrimaryStorageFactory implements NfsPrimaryStorageManager, PrimaryStorageFactory, Component, CreateTemplateFromVolumeSnapshotExtensionPoint, RecalculatePrimaryStorageCapacityExtensionPoint,
        PrimaryStorageDetachExtensionPoint, HostDeleteExtensionPoint{
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PrimaryStorageManager psMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    private Map<String, NfsPrimaryStorageBackend> backends = new HashMap<String, NfsPrimaryStorageBackend>();
    private Map<String, Map<String, NfsPrimaryToBackupStorageMediator>> mediators =
            new HashMap<>();

    private static final PrimaryStorageType type = new PrimaryStorageType(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);

    static {
        type.setSupportHeartbeatFile(true);
        type.setSupportPingStorageGateway(true);
        type.setOrder(899);
    }

    @VmExpungeRootVolumeValidator.VmExpungeRootVolumeValidatorMethod
    static void vmExpungeRootVolumeValidator(String vmUuid, String volumeUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                String psUuid = q(VolumeVO.class).select(VolumeVO_.primaryStorageUuid).eq(VolumeVO_.uuid, volumeUuid)
                        .findValue();

                if (psUuid == null) {
                    return;
                }

                if (!q(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, psUuid)
                        .eq(PrimaryStorageVO_.type, NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)
                        .isExists()) {
                    // not NFS
                    return;
                }

                if (!q(PrimaryStorageClusterRefVO.class).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuid).isExists()) {
                    throw new OperationFailureException(operr("the NFS primary storage[uuid:%s] is not attached" +
                            " to any clusters, and cannot expunge the root volume[uuid:%s] of the VM[uuid:%s]", psUuid, vmUuid, volumeUuid));
                }
            }
        }.execute();
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        String mountPathBase = NfsPrimaryStorageGlobalConfig.MOUNT_BASE.value(String.class);
        if (mountPathBase == null) {
            mountPathBase = NfsPrimaryStorageConstant.DEFAULT_NFS_MOUNT_PATH_ON_HOST;
        }
        String mountPath = PathUtil.join(mountPathBase, "prim-" + vo.getUuid());
        vo.setMountPath(mountPath);
        vo = dbf.persistAndRefresh(vo);

        SystemTagCreator creator = PrimaryStorageSystemTags.CAPABILITY_HYPERVISOR_SNAPSHOT.newSystemTagCreator(vo.getUuid());
        creator.setTagByTokens(map(
                e(PrimaryStorageSystemTags.CAPABILITY_HYPERVISOR_SNAPSHOT_TOKEN, KVMConstant.KVM_HYPERVISOR_TYPE)
        ));
        creator.create();

        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new NfsPrimaryStorage(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        PrimaryStorageVO vo = dbf.findByUuid(uuid, PrimaryStorageVO.class);
        return PrimaryStorageInventory.valueOf(vo);
    }

    private void populateExtensions() {
        for (NfsPrimaryStorageBackend extp : pluginRgty.getExtensionList(NfsPrimaryStorageBackend.class)) {
            NfsPrimaryStorageBackend old = backends.get(extp.getHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NfsPrimaryStorageBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), old.getHypervisorType()));
            }
            backends.put(extp.getHypervisorType().toString(), extp);
        }

        for (NfsPrimaryToBackupStorageMediator extp : pluginRgty.getExtensionList(NfsPrimaryToBackupStorageMediator.class)) {
            if (extp.getSupportedPrimaryStorageType().equals(type.toString())) {
                Map<String, NfsPrimaryToBackupStorageMediator> map = mediators.get(extp.getSupportedBackupStorageType());
                if (map == null) {
                    map = new HashMap<>(1);
                }
                for (String hvType : extp.getSupportedHypervisorTypes()) {
                    map.put(hvType, extp);
                }
                mediators.put(extp.getSupportedBackupStorageType(), map);
            }
        }
    }

    public NfsPrimaryToBackupStorageMediator getPrimaryToBackupStorageMediator(BackupStorageType bsType, HypervisorType hvType) {
        Map<String, NfsPrimaryToBackupStorageMediator> mediatorMap = mediators.get(bsType.toString());
        if (mediatorMap == null) {
            throw new CloudRuntimeException(
                    String.format("primary storage[type:%s] wont have mediator supporting backup storage[type:%s]", type, bsType));
        }
        NfsPrimaryToBackupStorageMediator mediator = mediatorMap.get(hvType.toString());
        if (mediator == null) {
            throw new CloudRuntimeException(
                    String.format("PrimaryToBackupStorageMediator[primary storage type: %s, backup storage type: %s] doesn't have backend supporting hypervisor type[%s]", type, bsType, hvType));
        }
        return mediator;
    }

    @Override
    public boolean start() {
        populateExtensions();
        NfsPrimaryStorageGlobalConfig.MOUNT_BASE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                if (!value.startsWith("/")) {
                    throw new GlobalConfigException(String.format("%s must be an absolute path starting with '/'", NfsPrimaryStorageGlobalConfig.MOUNT_BASE.getCanonicalName()));
                }
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public NfsPrimaryStorageBackend getHypervisorBackend(HypervisorType hvType) {
        NfsPrimaryStorageBackend backend = backends.get(hvType.toString());
        if (backend == null) {
            throw new CloudRuntimeException(String.format("Cannot find hypervisor backend for nfs primary storage supporting hypervisor type[%s]", hvType));
        }
        return backend;
    }

    @Transactional
    public HostInventory getConnectedHostForOperation(PrimaryStorageInventory pri) {
        if (pri.getAttachedClusterUuids().isEmpty()) {
            throw new OperationFailureException(operr("cannot find a Connected host to execute command for nfs primary storage[uuid:%s]", pri.getUuid()));
        }

        String sql = "select h from HostVO h where h.state = :state and h.status = :connectionState and h.clusterUuid in (:clusterUuids)";
        TypedQuery<HostVO> q = dbf.getEntityManager().createQuery(sql, HostVO.class);
        q.setParameter("state", HostState.Enabled);
        q.setParameter("connectionState", HostStatus.Connected);
        q.setParameter("clusterUuids", pri.getAttachedClusterUuids());
        q.setMaxResults(1);
        List<HostVO> ret = q.getResultList();
        if (ret.isEmpty()) {
            throw new OperationFailureException(operr("cannot find a Connected host to execute command for nfs primary storage[uuid:%s]", pri.getUuid()));
        } else {
            Collections.shuffle(ret);
            return HostInventory.valueOf(ret.get(0));
        }
    }

    @Override
    public void reportCapacityIfNeeded(String psUuid, NfsPrimaryStorageAgentResponse rsp) {
        if (rsp.getAvailableCapacity() != null && rsp.getTotalCapacity() != null) {
            new PrimaryStorageCapacityUpdater(psUuid).updateAvailablePhysicalCapacity(rsp.getAvailableCapacity());
        }
    }

    @Override
    public HypervisorType findHypervisorTypeByImageFormatAndPrimaryStorageUuid(String imageFormat, final String psUuid) {
        HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(imageFormat);
        if (hvType != null) {
            return hvType;
        }

        String type = new Callable<String>() {
            @Override
            @Transactional(readOnly = true)
            public String call() {
                String sql = "select c.hypervisorType" +
                        " from ClusterVO c, PrimaryStorageClusterRefVO ref" +
                        " where c.uuid = ref.clusterUuid" +
                        " and ref.primaryStorageUuid = :psUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("psUuid", psUuid);
                List<String> types = q.getResultList();
                return types.isEmpty() ? null : types.get(0);
            }
        }.call();

        if (type != null) {
            return HypervisorType.valueOf(type);
        }

        throw new OperationFailureException(operr("cannot find proper hypervisorType for primary storage[uuid:%s] to handle image format or volume format[%s]", psUuid, imageFormat));
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(final ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();

        final HypervisorType hvtype = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(paramIn.getSnapshot().getFormat());

        class Context {
            String tempInstallPath;
        }

        final Context ctx = new Context();

        template.setCreateTemporaryTemplate(new Flow() {
            String __name__ = "create-temporary-template";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);

                CreateTemporaryVolumeFromSnapshotMsg msg = new CreateTemporaryVolumeFromSnapshotMsg();
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setSnapshot(paramIn.getSnapshot());
                msg.setTemporaryVolumeUuid(paramIn.getImage().getUuid());
                msg.setHypervisorType(hvtype.toString());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            CreateTemporaryVolumeFromSnapshotReply r = reply.castReply();
                            ctx.tempInstallPath = r.getInstallPath();
                            out.setActualSize(r.getActualSize());
                            out.setSize(r.getSize());
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.tempInstallPath != null) {
                    DeleteBitsOnPrimaryStorageMsg msg = new DeleteBitsOnPrimaryStorageMsg();
                    msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                    msg.setInstallPath(ctx.tempInstallPath);
                    msg.setHypervisorType(hvtype.toString());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setUploadToBackupStorage(new Flow() {
            String __name__ = "upload-to-backup-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);

                BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();
                ask.setImageUuid(paramIn.getImage().getUuid());
                ask.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                ask.setImageMediaType(paramIn.getImage().getMediaType());
                bus.makeTargetServiceIdByResourceUuid(ask, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                MessageReply ar = bus.call(ask);
                if (!ar.isSuccess()) {
                    trigger.fail(ar.getError());
                    return;
                }

                String bsInstallPath = ((BackupStorageAskInstallPathReply) ar).getInstallPath();

                UploadBitsToBackupStorageMsg msg = new UploadBitsToBackupStorageMsg();
                msg.setHypervisorType(hvtype.toString());
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setPrimaryStorageInstallPath(ctx.tempInstallPath);
                msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                msg.setBackupStorageInstallPath(bsInstallPath);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());

                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            UploadBitsToBackupStorageReply r = reply.castReply();
                            out.setBackupStorageInstallPath(r.getBackupStorageInstallPath());
                            trigger.next();
                        }
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
                    bus.send(msg);
                }

                trigger.rollback();
            }
        });

        template.setDeleteTemporaryTemplate(new NoRollbackFlow() {
            String __name__ = "delete-temporary-template";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteBitsOnPrimaryStorageMsg msg = new DeleteBitsOnPrimaryStorageMsg();
                msg.setHypervisorType(hvtype.toString());
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setInstallPath(ctx.tempInstallPath);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg);

                trigger.next();
            }
        });

        return template;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String getPrimaryStorageTypeForRecalculateCapacityExtensionPoint() {
        return type.toString();
    }

    @Override
    public void beforeRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct) {
        // do nothing
        return;
    }

    @Override
    public void afterRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct) {
        if(isNfsUnmounted(struct.getPrimaryStorageUuid())){
            resetDefaultCapacityWhenNfsUnmounted(struct.getPrimaryStorageUuid());
        }
    }

    private boolean isNfsUnmounted(String psUuid) {
        long count = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuid).count();

        return count == 0;
    }

    private void resetDefaultCapacityWhenNfsUnmounted(String psUuid) {
        PrimaryStorageCapacityUpdater pupdater = new PrimaryStorageCapacityUpdater(psUuid);

        long  totalCapacity = 0;
        long  availableCapacity = 0;
        long  totalPhysicalCapacity = 0;
        long  availablePhysicalCapacity = 0;
        pupdater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                cap.setTotalCapacity(totalCapacity);
                cap.setAvailableCapacity(availableCapacity);
                cap.setTotalPhysicalCapacity(totalPhysicalCapacity);
                cap.setAvailablePhysicalCapacity(availablePhysicalCapacity);
                return cap;
            }
        });
    }

    @Override
    public void preDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException {
        return;
    }

    @Override
    public void beforeDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        return;
    }

    @Override
    public void failToDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        return;
    }

    @Override
    public void afterDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
        rmsg.setPrimaryStorageUuid(inventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, inventory.getUuid());
        bus.send(rmsg);
    }

    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {

    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        String clusterUuid = inventory.getClusterUuid();
        List<String> psUuids = getNfsPrimaryStorageInCluster(clusterUuid);
        if(psUuids == null || psUuids.isEmpty()) {
            return;
        }

        if (Q.New(HostVO.class).eq(HostVO_.clusterUuid, clusterUuid).notEq(HostVO_.uuid, inventory.getUuid()).isExists()) {
            return;
        }

        for(String psUuid : psUuids) {
            releasePrimaryStorageCapacity(psUuid);
        }
    }

    private void releasePrimaryStorageCapacity(String psUuid) {
        NfsRecalculatePrimaryStorageCapacityMsg msg = new NfsRecalculatePrimaryStorageCapacityMsg();
        msg.setPrimaryStorageUuid(psUuid);
        msg.setRelease(true);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(msg);
    }

    private List<String> getNfsPrimaryStorageInCluster(String clusterUuid) {
        return SQL.New("select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype")
                .param("cuuid", clusterUuid)
                .param("ptype", NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)
                .list();
    }
}
