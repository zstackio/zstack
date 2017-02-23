package org.zstack.storage.fusionstor.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.*;
import org.zstack.storage.fusionstor.*;
import org.zstack.storage.fusionstor.primary.KVMFusionstorVolumeTO.MonInfo;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 7/28/2015.
 */
public class FusionstorPrimaryStorageFactory implements PrimaryStorageFactory, FusionstorCapacityUpdateExtensionPoint, KVMStartVmExtensionPoint,
        KVMAttachVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(FusionstorPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(FusionstorConstants.FUSIONSTOR_PRIMARY_STORAGE_TYPE);

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
        APIAddFusionstorPrimaryStorageMsg cmsg = (APIAddFusionstorPrimaryStorageMsg) msg;

        FusionstorPrimaryStorageVO cvo = new FusionstorPrimaryStorageVO(vo);
        cvo.setType(FusionstorConstants.FUSIONSTOR_PRIMARY_STORAGE_TYPE);
        cvo.setMountPath(FusionstorConstants.FUSIONSTOR_PRIMARY_STORAGE_TYPE);
        cvo.setRootVolumePoolName(cmsg.getRootVolumePoolName() == null ? String.format("pri-v-r-%s", vo.getUuid()) : cmsg.getRootVolumePoolName());
        cvo.setDataVolumePoolName(cmsg.getDataVolumePoolName() == null ? String.format("pri-v-d-%s", vo.getUuid()) : cmsg.getDataVolumePoolName());
        cvo.setImageCachePoolName(cmsg.getImageCachePoolName() == null ? String.format("pri-c-%s", vo.getUuid()) : cmsg.getImageCachePoolName());

        dbf.getEntityManager().persist(cvo);

        if (cmsg.getImageCachePoolName() != null) {
            FusionstorSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.createInherentTag(cvo.getUuid());
        }
        if (cmsg.getRootVolumePoolName() != null) {
            FusionstorSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.createInherentTag(cvo.getUuid());
        }
        if (cmsg.getDataVolumePoolName() != null) {
            FusionstorSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.createInherentTag(cvo.getUuid());
        }

        for (String url : cmsg.getMonUrls()) {
            FusionstorPrimaryStorageMonVO mvo = new FusionstorPrimaryStorageMonVO();
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

        FusionstorSystemTags.KVM_SECRET_UUID.recreateInherentTag(vo.getUuid(), PrimaryStorageVO.class,
                map(e(FusionstorSystemTags.KVM_SECRET_UUID_TOKEN, UUID.randomUUID().toString()))
        );

        return PrimaryStorageInventory.valueOf(cvo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        FusionstorPrimaryStorageVO cvo = dbf.findByUuid(vo.getUuid(), FusionstorPrimaryStorageVO.class);
        return new FusionstorPrimaryStorageBase(cvo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return FusionstorPrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, FusionstorPrimaryStorageVO.class));
    }

    @Override
    public void update(String fsid, long total, long avail) {
        String sql = "select cap from PrimaryStorageCapacityVO cap, FusionstorPrimaryStorageVO pri where pri.uuid = cap.uuid and pri.fsid = :fsid";
        TypedQuery<PrimaryStorageCapacityVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageCapacityVO.class);
        q.setParameter("fsid", fsid);
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(q);
        updater.update(total, avail, total, avail);
    }

    private IsoTO convertIsoToFusionstorIfNeeded(final IsoTO to) {
        if (to == null || !to.getPath().startsWith(VolumeTO.FUSIONSTOR)) {
            return to;
        }

        FusionstorPrimaryStorageVO pri = new Callable<FusionstorPrimaryStorageVO>() {
            @Override
            @Transactional(readOnly = true)
            public FusionstorPrimaryStorageVO call() {
                String sql = "select pri from FusionstorPrimaryStorageVO pri, ImageCacheVO c where pri.uuid = c.primaryStorageUuid" +
                        " and c.imageUuid = :imgUuid";
                TypedQuery<FusionstorPrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, FusionstorPrimaryStorageVO.class);
                q.setParameter("imgUuid", to.getImageUuid());
                return q.getSingleResult();
            }
        }.call();

        KvmFusionstorIsoTO cto = new KvmFusionstorIsoTO(to);
        cto.setMonInfo(CollectionUtils.transformToList(pri.getMons(), new Function<KvmFusionstorIsoTO.MonInfo, FusionstorPrimaryStorageMonVO>() {
            @Override
            public KvmFusionstorIsoTO.MonInfo call(FusionstorPrimaryStorageMonVO arg) {
                if (MonStatus.Connected != arg.getStatus()) {
                    return null;
                }

                KvmFusionstorIsoTO.MonInfo info = new KvmFusionstorIsoTO.MonInfo();
                info.setHostname(arg.getHostname());
                info.setPort(arg.getMonPort());
                return info;
            }
        }));

        if (cto.getMonInfo().isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find any Connected fusionstor mon for the primary storage[uuid:%s]", pri.getUuid())
            ));
        }

	/*
        String secretUuid = FusionstorSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(pri.getUuid(), FusionstorSystemTags.KVM_SECRET_UUID_TOKEN);
        if (secretUuid == null) {
            throw new CloudRuntimeException(String.format("cannot find KVM secret uuid for fusionstor primary storage[uuid:%s]", pri.getUuid()));
        }
        cto.setSecretUuid(secretUuid);
	*/

        return cto;
    }

    private VolumeTO convertVolumeToFusionstorIfNeeded(VolumeInventory vol, VolumeTO to) {
        if (!vol.getInstallPath().startsWith(VolumeTO.FUSIONSTOR)) {
            return to;
        }

        SimpleQuery<FusionstorPrimaryStorageMonVO> q = dbf.createQuery(FusionstorPrimaryStorageMonVO.class);
        q.select(FusionstorPrimaryStorageMonVO_.hostname, FusionstorPrimaryStorageMonVO_.monPort);
        q.add(FusionstorPrimaryStorageMonVO_.primaryStorageUuid, Op.EQ, vol.getPrimaryStorageUuid());
        q.add(FusionstorPrimaryStorageMonVO_.status, Op.EQ, MonStatus.Connected);
        List<Tuple> ts = q.listTuple();

        if (ts.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find any Connected fusionstor mon for the primary storage[uuid:%s]", vol.getPrimaryStorageUuid())
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

	/*
        String secretUuid = FusionstorSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(vol.getPrimaryStorageUuid(), FusionstorSystemTags.KVM_SECRET_UUID_TOKEN);
        if (secretUuid == null) {
            throw new CloudRuntimeException(String.format("cannot find KVM secret uuid for fusionstor primary storage[uuid:%s]", vol.getPrimaryStorageUuid()));
        }
	*/

        KVMFusionstorVolumeTO cto = new KVMFusionstorVolumeTO(to);
        //cto.setSecretUuid(secretUuid);
        cto.setMonInfo(monInfos);
        cto.setDeviceType(VolumeTO.FUSIONSTOR);
        return cto;
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeToFusionstorIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeToFusionstorIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {

    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) throws KVMException {
        cmd.setRootVolume(convertVolumeToFusionstorIfNeeded(spec.getDestRootVolume(), cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<VolumeTO>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            VolumeInventory dvol = null;
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dvol = vol;
                    break;
                }
            }

            dtos.add(convertVolumeToFusionstorIfNeeded(dvol, to));
        }

        cmd.setDataVolumes(dtos);
        cmd.setBootIso(convertIsoToFusionstorIfNeeded(cmd.getBootIso()));
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
            asf.deployModule(FusionstorGlobalProperty.PRIMARY_STORAGE_MODULE_PATH, FusionstorGlobalProperty.PRIMARY_STORAGE_PLAYBOOK_NAME);
        }

        startCleanupThread();

        FusionstorGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
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
                return FusionstorGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "fusionstor-primary-storage-image-cleanup-thread";
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
                q.setParameter("ptype", FusionstorConstants.FUSIONSTOR_PRIMARY_STORAGE_TYPE);
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
}
