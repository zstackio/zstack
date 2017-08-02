package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.SyncVolumeSizeMsg;
import org.zstack.header.volume.SyncVolumeSizeReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.*;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.primary.KVMCephVolumeTO.MonInfo;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.snapshot.PostMarkRootVolumeAsSnapshotExtension;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageFactory implements PrimaryStorageFactory, CephCapacityUpdateExtensionPoint, KVMStartVmExtensionPoint,
        KVMAttachVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, CreateTemplateFromVolumeSnapshotExtensionPoint,
        KvmSetupSelfFencerExtensionPoint, KVMPreAttachIsoExtensionPoint, Component, PostMarkRootVolumeAsSnapshotExtension {
    private static final CLogger logger = Utils.getLogger(CephPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;

    private Future imageCacheCleanupThread;

    static {
        type.setSupportHeartbeatFile(true);
        type.setSupportPingStorageGateway(true);
        type.setOrder(799);
    }

    void init() {
        type.setPrimaryStorageFindBackupStorage(new PrimaryStorageFindBackupStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findBackupStorage(String primaryStorageUuid) {
                String sql = "select b.uuid from CephPrimaryStorageVO p, CephBackupStorageVO b where b.fsid = p.fsid" +
                        " and p.uuid = :puuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("puuid", primaryStorageUuid);
                return q.getResultList();
            }
        });
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    @Transactional
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        APIAddCephPrimaryStorageMsg cmsg = (APIAddCephPrimaryStorageMsg) msg;

        CephPrimaryStorageVO cvo = new CephPrimaryStorageVO(vo);
        cvo.setType(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
        cvo.setMountPath(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
        cvo.setRootVolumePoolName(cmsg.getRootVolumePoolName() == null ? String.format("pri-v-r-%s", vo.getUuid()) : cmsg.getRootVolumePoolName());
        cvo.setDataVolumePoolName(cmsg.getDataVolumePoolName() == null ? String.format("pri-v-d-%s", vo.getUuid()) : cmsg.getDataVolumePoolName());
        cvo.setImageCachePoolName(cmsg.getImageCachePoolName() == null ? String.format("pri-c-%s", vo.getUuid()) : cmsg.getImageCachePoolName());

        dbf.getEntityManager().persist(cvo);

        if (cmsg.getImageCachePoolName() != null) {
            SystemTagCreator creator = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }
        if (cmsg.getRootVolumePoolName() != null) {
            SystemTagCreator creator = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }
        if (cmsg.getDataVolumePoolName() != null) {
            SystemTagCreator creator = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }

        for (String url : cmsg.getMonUrls()) {
            CephPrimaryStorageMonVO mvo = new CephPrimaryStorageMonVO();
            MonUri uri = new MonUri(url);
            mvo.setUuid(Platform.getUuid());
            mvo.setStatus(MonStatus.Connecting);
            mvo.setHostname(uri.getHostname());
            mvo.setMonAddr(mvo.getHostname());
            mvo.setMonPort(uri.getMonPort());
            mvo.setSshPort(uri.getSshPort());
            mvo.setSshUsername(uri.getSshUsername());
            mvo.setSshPassword(uri.getSshPassword());
            mvo.setPrimaryStorageUuid(cvo.getUuid());
            dbf.getEntityManager().persist(mvo);
        }

        SystemTagCreator creator = CephSystemTags.KVM_SECRET_UUID.newSystemTagCreator(vo.getUuid());
        creator.setTagByTokens(map(e(CephSystemTags.KVM_SECRET_UUID_TOKEN, UUID.randomUUID().toString())));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();

        return PrimaryStorageInventory.valueOf(cvo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        CephPrimaryStorageVO cvo = dbf.findByUuid(vo.getUuid(), CephPrimaryStorageVO.class);
        return new CephPrimaryStorageBase(cvo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return CephPrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, CephPrimaryStorageVO.class));
    }

    @Override
    public void update(String fsid, final long total, final long avail) {
        String sql = "select cap from PrimaryStorageCapacityVO cap, CephPrimaryStorageVO pri where pri.uuid = cap.uuid and pri.fsid = :fsid";
        TypedQuery<PrimaryStorageCapacityVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageCapacityVO.class);
        q.setParameter("fsid", fsid);
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(q);
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                if (cap.getTotalCapacity() == 0 && cap.getAvailableCapacity() == 0) {
                    // init
                    cap.setTotalCapacity(total);
                    cap.setAvailableCapacity(avail);
                }

                cap.setTotalPhysicalCapacity(total);
                cap.setAvailablePhysicalCapacity(avail);

                return cap;
            }
        });
    }

    private IsoTO convertIsoToCephIfNeeded(final IsoTO to) {
        if (to == null || !to.getPath().startsWith(VolumeTO.CEPH)) {
            return to;
        }

        CephPrimaryStorageVO pri = new Callable<CephPrimaryStorageVO>() {
            @Override
            @Transactional(readOnly = true)
            public CephPrimaryStorageVO call() {
                String sql = "select pri from CephPrimaryStorageVO pri, ImageCacheVO c where pri.uuid = c.primaryStorageUuid" +
                        " and c.imageUuid = :imgUuid";
                TypedQuery<CephPrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, CephPrimaryStorageVO.class);
                q.setParameter("imgUuid", to.getImageUuid());
                return q.getSingleResult();
            }
        }.call();

        KvmCephIsoTO cto = new KvmCephIsoTO(to);
        cto.setMonInfo(CollectionUtils.transformToList(pri.getMons(), new Function<KvmCephIsoTO.MonInfo, CephPrimaryStorageMonVO>() {
            @Override
            public KvmCephIsoTO.MonInfo call(CephPrimaryStorageMonVO arg) {
                if (MonStatus.Connected != arg.getStatus()) {
                    return null;
                }

                KvmCephIsoTO.MonInfo info = new KvmCephIsoTO.MonInfo();
                info.setHostname(arg.getMonAddr());
                info.setPort(arg.getMonPort());
                return info;
            }
        }));

        if (cto.getMonInfo().isEmpty()) {
            throw new OperationFailureException(operr(
                    "cannot find any Connected ceph mon for the primary storage[uuid:%s]", pri.getUuid()
            ));
        }

        String secretUuid = CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(pri.getUuid(), CephSystemTags.KVM_SECRET_UUID_TOKEN);
        if (secretUuid == null) {
            throw new CloudRuntimeException(String.format("cannot find KVM secret uuid for ceph primary storage[uuid:%s]", pri.getUuid()));
        }
        cto.setSecretUuid(secretUuid);

        return cto;
    }

    private VolumeTO convertVolumeToCephIfNeeded(VolumeInventory vol, VolumeTO to) {
        if (!vol.getInstallPath().startsWith(VolumeTO.CEPH)) {
            return to;
        }

        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.select(CephPrimaryStorageMonVO_.monAddr, CephPrimaryStorageMonVO_.monPort);
        q.add(CephPrimaryStorageMonVO_.primaryStorageUuid, Op.EQ, vol.getPrimaryStorageUuid());
        q.add(CephPrimaryStorageMonVO_.status, Op.EQ, MonStatus.Connected);
        List<Tuple> ts = q.listTuple();

        if (ts.isEmpty()) {
            throw new OperationFailureException(operr(
                    "cannot find any Connected ceph mon for the primary storage[uuid:%s]", vol.getPrimaryStorageUuid())
            );
        }

        List<MonInfo> monInfos = CollectionUtils.transformToList(ts, new Function<MonInfo, Tuple>() {
            @Override
            public MonInfo call(Tuple t) {
                String hostname = t.get(0, String.class);
                DebugUtils.Assert(hostname != null, "hostname cannot be null");

                int port = t.get(1, Integer.class);

                MonInfo info = new MonInfo();
                info.hostname = hostname;
                info.port = port;
                return info;
            }
        });


        String secretUuid = CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(vol.getPrimaryStorageUuid(), CephSystemTags.KVM_SECRET_UUID_TOKEN);
        if (secretUuid == null) {
            throw new CloudRuntimeException(String.format("cannot find KVM secret uuid for ceph primary storage[uuid:%s]", vol.getPrimaryStorageUuid()));
        }

        KVMCephVolumeTO cto = new KVMCephVolumeTO(to);
        cto.setSecretUuid(secretUuid);
        cto.setMonInfo(monInfos);
        cto.setDeviceType(VolumeTO.CEPH);
        return cto;
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeToCephIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeToCephIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {

    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) throws KVMException {
        cmd.setRootVolume(convertVolumeToCephIfNeeded(spec.getDestRootVolume(), cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<VolumeTO>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            VolumeInventory dvol = null;
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dvol = vol;
                    break;
                }
            }

            dtos.add(convertVolumeToCephIfNeeded(dvol, to));
        }

        cmd.setDataVolumes(dtos);
        cmd.setBootIso(convertIsoToCephIfNeeded(cmd.getBootIso()));

        CephPrimaryStorageVO cephPrimaryStorageVO = dbf.findByUuid(spec.getDestRootVolume().getPrimaryStorageUuid(), CephPrimaryStorageVO.class);
        if(cephPrimaryStorageVO != null){
            cmd.getAddons().put(CephConstants.CEPH_SCECRET_KEY, cephPrimaryStorageVO.getUserKey());
            cmd.getAddons().put(CephConstants.CEPH_SECRECT_UUID, CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(cephPrimaryStorageVO.getUuid(), CephSystemTags.KVM_SECRET_UUID_TOKEN));
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public boolean start() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            asf.deployModule(CephGlobalProperty.PRIMARY_STORAGE_MODULE_PATH, CephGlobalProperty.PRIMARY_STORAGE_PLAYBOOK_NAME);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(final ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();
        template.setCreateTemporaryTemplate(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                msg.setVolumeUuid(paramIn.getSnapshot().getVolumeUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, paramIn.getSnapshot().getVolumeUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            ParamOut paramOut = (ParamOut) data.get(ParamOut.class);
                            SyncVolumeSizeReply gr = reply.castReply();
                            paramOut.setActualSize(gr.getActualSize());
                            paramOut.setSize(gr.getSize());
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
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

                String bsInstallPath = ((BackupStorageAskInstallPathReply)ar).getInstallPath();

                UploadBitsToBackupStorageMsg msg = new UploadBitsToBackupStorageMsg();
                msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                msg.setPrimaryStorageInstallPath(paramIn.getSnapshot().getPrimaryStorageInstallPath());
                msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                msg.setBackupStorageInstallPath(bsInstallPath);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());

                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            out.setBackupStorageInstallPath(bsInstallPath);
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

        template.setDeleteTemporaryTemplate(new NopeFlow());

        return template;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String kvmSetupSelfFencerStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void kvmSetupSelfFencer(KvmSetupSelfFencerParam param, final Completion completion) {
        SetupSelfFencerOnKvmHostMsg msg = new SetupSelfFencerOnKvmHostMsg();
        msg.setParam(param);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void kvmCancelSelfFencer(KvmCancelSelfFencerParam param, Completion completion) {
        CancelSelfFencerOnKvmHostMsg msg = new CancelSelfFencerOnKvmHostMsg();
        msg.setParam(param);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void preAttachIsoExtensionPoint(KVMHostInventory host, AttachIsoCmd cmd) {
        cmd.iso = convertIsoToCephIfNeeded(cmd.iso);
    }

    @Override
    public void afterMarkRootVolumeAsSnapshot(VolumeSnapshotInventory snapshot) {

    }
}
