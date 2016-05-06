package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
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
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.SyncVolumeSizeMsg;
import org.zstack.header.volume.SyncVolumeSizeReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.*;
import org.zstack.storage.backup.BackupStorageCapacityUpdater;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.primary.KVMCephVolumeTO.MonInfo;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.CollectionUtils;
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
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageFactory implements PrimaryStorageFactory, CephCapacityUpdateExtensionPoint, KVMStartVmExtensionPoint,
        KVMAttachVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, CreateTemplateFromVolumeSnapshotExtensionPoint, Component {
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
            CephSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.createInherentTag(cvo.getUuid());
        }
        if (cmsg.getRootVolumePoolName() != null) {
            CephSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.createInherentTag(cvo.getUuid());
        }
        if (cmsg.getDataVolumePoolName() != null) {
            CephSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.createInherentTag(cvo.getUuid());
        }

        for (String url : cmsg.getMonUrls()) {
            CephPrimaryStorageMonVO mvo = new CephPrimaryStorageMonVO();
            MonUri uri = new MonUri(url);
            mvo.setUuid(Platform.getUuid());
            mvo.setStatus(MonStatus.Connecting);
            mvo.setHostname(uri.getHostname());
            mvo.setMonPort(uri.getMonPort());
            mvo.setSshPort(uri.getSshPort());
            mvo.setSshUsername(uri.getSshUsername());
            mvo.setSshPassword(uri.getSshPassword());
            mvo.setPrimaryStorageUuid(cvo.getUuid());
            dbf.getEntityManager().persist(mvo);
        }

        CephSystemTags.KVM_SECRET_UUID.recreateInherentTag(vo.getUuid(), PrimaryStorageVO.class,
                map(e(CephSystemTags.KVM_SECRET_UUID_TOKEN, UUID.randomUUID().toString()))
        );

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
                info.setHostname(arg.getHostname());
                info.setPort(arg.getMonPort());
                return info;
            }
        }));

        if (cto.getMonInfo().isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find any Connected ceph mon for the primary storage[uuid:%s]", pri.getUuid())
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
        q.select(CephPrimaryStorageMonVO_.hostname, CephPrimaryStorageMonVO_.monPort);
        q.add(CephPrimaryStorageMonVO_.primaryStorageUuid, Op.EQ, vol.getPrimaryStorageUuid());
        q.add(CephPrimaryStorageMonVO_.status, Op.EQ, MonStatus.Connected);
        List<Tuple> ts = q.listTuple();

        if (ts.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find any Connected ceph mon for the primary storage[uuid:%s]", vol.getPrimaryStorageUuid())
            ));
        }

        List<MonInfo> monInfos = CollectionUtils.transformToList(ts, new Function<MonInfo, Tuple>() {
            @Override
            public MonInfo call(Tuple t) {
                String hostname = t.get(0, String.class);
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

        startCleanupThread();

        CephGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (imageCacheCleanupThread != null) {
                    imageCacheCleanupThread.cancel(true);
                }
                startCleanupThread();
            }
        });

        return true;
    }

    private void startCleanupThread() {
        imageCacheCleanupThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return CephGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "ceph-primary-storage-image-cleanup-thread";
            }

            @Override
            public void run() {
                List<ImageCacheVO> staleCache = getStaleCache();
                if (staleCache == null || staleCache.isEmpty()) {
                    return;
                }

                for (final ImageCacheVO c : staleCache) {
                    DeleteBitsOnPrimaryStorageMsg msg = new DeleteBitsOnPrimaryStorageMsg();
                    msg.setInstallPath(c.getInstallUrl().split("@")[0]);
                    msg.setPrimaryStorageUuid(c.getPrimaryStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, c.getPrimaryStorageUuid());
                    bus.send(msg, new CloudBusCallBack() {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("successfully cleanup a stale image cache[path:%s, primary storage uuid:%s]", c.getInstallUrl(), c.getPrimaryStorageUuid()));
                                dbf.remove(c);
                            } else {
                                logger.warn(String.format("failed to cleanup a stale image cache[path:%s, primary storage uuid:%s], %s", c.getInstallUrl(), c.getPrimaryStorageUuid(), reply.getError()));
                            }
                        }
                    });
                }
            }

            @Transactional(readOnly = true)
            private List<ImageCacheVO> getStaleCache() {
                String sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where ((c.imageUuid is null) or (i.uuid = c.imageUuid and i.deleted is not null)) and " +
                        "pri.type = :ptype and pri.uuid = c.primaryStorageUuid";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("ptype", CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
                List<Long> ids = q.getResultList();
                if (ids.isEmpty()) {
                    return null;
                }

                sql = "select ref.imageCacheId from ImageCacheVolumeRefVO ref where ref.imageCacheId in (:ids)";
                TypedQuery<Long> refq = dbf.getEntityManager().createQuery(sql, Long.class);
                refq.setParameter("ids", ids);
                List<Long> existing = refq.getResultList();

                ids.removeAll(existing);

                if (ids.isEmpty()) {
                    return null;
                }

                sql = "select c from ImageCacheVO c where c.id in (:ids)";
                TypedQuery<ImageCacheVO> fq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
                fq.setParameter("ids", ids);
                return fq.getResultList();
            }
        });
    }

    @Override
    public boolean stop() {
        if (imageCacheCleanupThread != null) {
            imageCacheCleanupThread.cancel(true);
        }
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

            @AfterDone
            List<Runnable> returnCapacityToBackupStorage = new ArrayList<Runnable>();

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                final List<String> bsUuids = paramIn.getSelectedBackupStorageUuids();
                List<ErrorCode> errors = new ArrayList<ErrorCode>();
                final List<UploadBitsToBackupStorageMsg> msgs = new ArrayList<UploadBitsToBackupStorageMsg>();

                for (final String bsUuid : bsUuids) {
                    BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();
                    ask.setImageUuid(paramIn.getImage().getUuid());
                    ask.setBackupStorageUuid(bsUuid);
                    ask.setImageMediaType(paramIn.getImage().getMediaType());
                    bus.makeTargetServiceIdByResourceUuid(ask, BackupStorageConstant.SERVICE_ID, bsUuid);
                    MessageReply ar = bus.call(ask);
                    if (!ar.isSuccess()) {
                        errors.add(ar.getError());

                        returnCapacityToBackupStorage.add(new Runnable() {
                            @Override
                            public void run() {
                                BackupStorageCapacityUpdater updater = new BackupStorageCapacityUpdater(bsUuid);
                                updater.increaseAvailableCapacity(out.getActualSize());
                            }
                        });

                        continue;
                    }

                    String bsInstallPath = ((BackupStorageAskInstallPathReply)ar).getInstallPath();

                    UploadBitsToBackupStorageMsg msg = new UploadBitsToBackupStorageMsg();
                    msg.setPrimaryStorageUuid(paramIn.getPrimaryStorageUuid());
                    msg.setPrimaryStorageInstallPath(paramIn.getSnapshot().getPrimaryStorageInstallPath());
                    msg.setBackupStorageUuid(bsUuid);
                    msg.setBackupStorageInstallPath(bsInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                    msgs.add(msg);
                }

                if (msgs.isEmpty()) {
                    trigger.fail(errf.stringToOperationError(
                            String.format("failed to get install path on all backup storage%s", bsUuids), errors
                    ));
                    return;
                }

                bus.send(msgs, new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        List<ErrorCode> errors = new ArrayList<ErrorCode>();
                        for (MessageReply reply : replies) {
                            final UploadBitsToBackupStorageMsg msg = msgs.get(replies.indexOf(reply));
                            if (!reply.isSuccess()) {
                                errors.add(reply.getError());

                                returnCapacityToBackupStorage.add(new Runnable() {
                                    @Override
                                    public void run() {
                                        BackupStorageCapacityUpdater updater = new BackupStorageCapacityUpdater(msg.getBackupStorageUuid());
                                        updater.increaseAvailableCapacity(out.getActualSize());
                                    }
                                });

                                continue;
                            }

                            BackupStorageResult res = new BackupStorageResult();
                            res.setBackupStorageUuid(msg.getBackupStorageUuid());
                            res.setInstallPath(msg.getBackupStorageInstallPath());
                            out.getBackupStorageResult().add(res);
                        }

                        if (out.getBackupStorageResult().isEmpty()) {
                            trigger.fail(errf.stringToOperationError(
                                    String.format("failed to upload to all backup storage%s", bsUuids), errors
                            ));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (!out.getBackupStorageResult().isEmpty()) {
                    for (BackupStorageResult res : out.getBackupStorageResult()) {
                        DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                        msg.setInstallPath(res.getInstallPath());
                        msg.setBackupStorageUuid(res.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, res.getBackupStorageUuid());
                        bus.send(msg);
                    }
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
}
