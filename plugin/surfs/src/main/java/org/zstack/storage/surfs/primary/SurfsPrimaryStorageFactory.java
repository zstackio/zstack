package org.zstack.storage.surfs.primary;

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
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.surfs.primary.KVMSurfsVolumeTO.NodeInfo;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.SyncVolumeSizeMsg;
import org.zstack.header.volume.SyncVolumeSizeReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.*;
import org.zstack.storage.surfs.*;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import java.lang.reflect.Field;
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
 * Created by zhouhaiping 2017-09-14
 */
public class SurfsPrimaryStorageFactory implements PrimaryStorageFactory, SurfsCapacityUpdateExtensionPoint, KVMStartVmExtensionPoint,
        KVMAttachVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, CreateTemplateFromVolumeSnapshotExtensionPoint, KvmSetupSelfFencerExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(SurfsPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE);

    public static final String QEMUPATH = "/opt/surfstack/qemu/bin/qemu-system-x86_64";
    
    private SurfsPrimaryStorageBase SurfsPbase;

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
    }

    void init() {
        type.setPrimaryStorageFindBackupStorage(new PrimaryStorageFindBackupStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findBackupStorage(String primaryStorageUuid) {
                String sql = "select b.uuid from SurfsPrimaryStorageVO p, SurfsBackupStorageVO b where b.fsid = p.fsid" +
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
        APIAddSurfsPrimaryStorageMsg cmsg = (APIAddSurfsPrimaryStorageMsg) msg;

        SurfsPrimaryStorageVO cvo = new SurfsPrimaryStorageVO(vo);
        cvo.setType(SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE);
        cvo.setMountPath(SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE);
        cvo.setRootVolumePoolName(cmsg.getRootVolumePoolName() == null ? String.format("pri-v-r-%s", vo.getUuid()) : cmsg.getRootVolumePoolName());
        cvo.setDataVolumePoolName(cmsg.getDataVolumePoolName() == null ? String.format("pri-v-d-%s", vo.getUuid()) : cmsg.getDataVolumePoolName());
        cvo.setImageCachePoolName(cmsg.getImageCachePoolName() == null ? String.format("pri-c-%s", vo.getUuid()) : cmsg.getImageCachePoolName());

        dbf.getEntityManager().persist(cvo);

        if (cmsg.getImageCachePoolName() != null) {
            SystemTagCreator creator = SurfsSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }
        if (cmsg.getRootVolumePoolName() != null) {
            SystemTagCreator creator = SurfsSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }
        if (cmsg.getDataVolumePoolName() != null) {
            SystemTagCreator creator = SurfsSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }

        for (String url : cmsg.getNodeUrls()) {
            SurfsPrimaryStorageNodeVO mvo = new SurfsPrimaryStorageNodeVO();
            NodeUri uri = new NodeUri(url);
            mvo.setUuid(Platform.getUuid());
            mvo.setStatus(NodeStatus.Connecting);
            mvo.setHostname(uri.getHostname());
            mvo.setNodeAddr(mvo.getHostname());
            mvo.setNodePort(uri.getNodePort());
            mvo.setSshPort(uri.getSshPort());
            mvo.setSshUsername(uri.getSshUsername());
            mvo.setSshPassword(uri.getSshPassword());
            mvo.setPrimaryStorageUuid(cvo.getUuid());
            dbf.getEntityManager().persist(mvo);
        }

        SystemTagCreator creator = SurfsSystemTags.KVM_SECRET_UUID.newSystemTagCreator(vo.getUuid());
        creator.inherent = true;
        creator.recreate = true;
        creator.setTagByTokens(map(e(SurfsSystemTags.KVM_SECRET_UUID_TOKEN, UUID.randomUUID().toString())));
        creator.create();

        return PrimaryStorageInventory.valueOf(cvo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        SurfsPrimaryStorageVO cvo = dbf.findByUuid(vo.getUuid(), SurfsPrimaryStorageVO.class);
        SurfsPbase = new SurfsPrimaryStorageBase(cvo);        
        return SurfsPbase;
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return SurfsPrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, SurfsPrimaryStorageVO.class));
    }

    @Override
    public void update(String fsid, final long total, final long avail) {
        String sql = "select cap from PrimaryStorageCapacityVO cap, SurfsPrimaryStorageVO pri where pri.uuid = cap.uuid and pri.fsid = :fsid";
        TypedQuery<PrimaryStorageCapacityVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageCapacityVO.class);
        q.setParameter("fsid", fsid);
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(q);
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
            	/*
                if (cap.getTotalCapacity() == 0 && cap.getAvailableCapacity() == 0) {
                    // init
                    cap.setTotalCapacity(total);
                    cap.setAvailableCapacity(avail);
                }
                */
                cap.setTotalCapacity(total);
                cap.setAvailableCapacity(avail);
                cap.setTotalPhysicalCapacity(total);
                cap.setAvailablePhysicalCapacity(avail);

                return cap;
            }
        });
    }


    private IsoTO convertIsoToSurfsIfNeeded(final IsoTO to) {
        if (to == null || !to.getPath().startsWith("iscsi")) {
            return to;
        }

        SurfsPrimaryStorageVO pri = new Callable<SurfsPrimaryStorageVO>() {
            @Override
            @Transactional(readOnly = true)
            public SurfsPrimaryStorageVO call() {
                String sql = "select pri from SurfsPrimaryStorageVO pri, ImageCacheVO c where pri.uuid = c.primaryStorageUuid" +
                        " and c.imageUuid = :imgUuid";
                TypedQuery<SurfsPrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, SurfsPrimaryStorageVO.class);
                q.setParameter("imgUuid", to.getImageUuid());
                return q.getSingleResult();
            }
        }.call();

        KvmSurfsIsoTO cto = new KvmSurfsIsoTO(to);
        cto.setNodeInfo(CollectionUtils.transformToList(pri.getNodes(), new Function<KvmSurfsIsoTO.NodeInfo, SurfsPrimaryStorageNodeVO>() {
            @Override
            public KvmSurfsIsoTO.NodeInfo call(SurfsPrimaryStorageNodeVO arg) {
                if (NodeStatus.Connected != arg.getStatus()) {
                    return null;
                }

                KvmSurfsIsoTO.NodeInfo info = new KvmSurfsIsoTO.NodeInfo();
                info.setHostname(arg.getHostname());
                info.setPort(arg.getNodePort());
                return info;
            }
        }));

        if (cto.getNodeInfo().isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find any Connected surfs mon for the primary storage[uuid:%s]", pri.getUuid())
            ));
        }

        return cto;
    }

    private VolumeTO convertVolumeToSurfsIfNeeded(VolumeInventory vol, VolumeTO to) {
    	
        if (!vol.getInstallPath().startsWith("iscsi")) {
            return to;
        }
        
        SimpleQuery<SurfsPrimaryStorageNodeVO> q = dbf.createQuery(SurfsPrimaryStorageNodeVO.class);
        q.select(SurfsPrimaryStorageNodeVO_.hostname, SurfsPrimaryStorageNodeVO_.nodePort);
        q.add(SurfsPrimaryStorageNodeVO_.primaryStorageUuid, Op.EQ, vol.getPrimaryStorageUuid());
        q.add(SurfsPrimaryStorageNodeVO_.status, Op.EQ, NodeStatus.Connected);
        List<Tuple> ts = q.listTuple();

        if (ts.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find any Connected surfs node for the primary storage[uuid:%s]", vol.getPrimaryStorageUuid())
            ));
        }

        List<NodeInfo> monInfos = CollectionUtils.transformToList(ts, new Function<NodeInfo, Tuple>() {
            @Override
            public NodeInfo call(Tuple t) {
                String hostname = t.get(0, String.class);
                int port = t.get(1, Integer.class);

                NodeInfo info = new NodeInfo();
                info.hostname = hostname;
                info.port = port;
                return info;
            }
        });

        KVMSurfsVolumeTO cto = new KVMSurfsVolumeTO(to);
        cto.setNodeInfo(monInfos);
        cto.setDeviceType("surfs");
        return cto;
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeToSurfsIfNeeded(volume, cmd.getVolume()));
        class VolType{
        	String voltype;
        	public String getVoltype(){
        		return this.voltype;
        	}
        	public void setVoltype(String vltype){
        		this.voltype=vltype;
        	}
        }
        VolType voltp=new VolType();
        try{
        	Field fds=volume.getClass().getDeclaredField("poolcls");
        	fds.setAccessible(true);
        	voltp.setVoltype(fds.get(volume).toString());
        }catch(Exception ex){
        	voltp.setVoltype("hdd");
        	logger.warn("Can not get volume pool class,now set common type to hdd ");
        }
        SurfsPbase.attachVolumeToVmPrepare(host.getManagementIp(), cmd.getVolume().getInstallPath(), cmd.getVolume().getVolumeUuid(),volume.getSize(),voltp.getVoltype());
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {
        
    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {
    	
        cmd.setVolume(convertVolumeToSurfsIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd) {
    	SurfsPbase.detachVolumeFromVmafter(host.getManagementIp(), cmd.getVolume().getInstallPath(), cmd.getVolume().getVolumeUuid());
    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, DetachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) throws KVMException {
    	VolumeInventory root = spec.getDestRootVolume();
    	String datavols="";
      
    	for (VolumeTO dvl : cmd.getDataVolumes()) {
    		if (datavols.equals("")){
    			datavols= dvl.getInstallPath().split("/")[2] + ":" + dvl.getVolumeUuid();   			
    		}else{
    			datavols= datavols + "," + dvl.getInstallPath().split("/")[2] + ":" + dvl.getVolumeUuid();
    		}
    	}
	
    	SurfsPbase.startVmBefore(host.getManagementIp(),root.getInstallPath(),root.getUuid(), cmd.getVmInstanceUuid(),datavols,25);
       
        if (!root.getInstallPath().startsWith("iscsi")) {
            return;
        }

        cmd.getAddons().put("qemuPath", QEMUPATH);
        cmd.setRootVolume(convertVolumeToSurfsIfNeeded(root, cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<VolumeTO>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            VolumeInventory dvol = null;
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dvol = vol;
                    break;
                }
            }

            dtos.add(convertVolumeToSurfsIfNeeded(dvol, to));
        }

        cmd.setDataVolumes(dtos);
        cmd.setBootIso(convertIsoToSurfsIfNeeded(cmd.getBootIso()));
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
            asf.deployModule(SurfsGlobalProperty.PRIMARY_STORAGE_MODULE_PATH, SurfsGlobalProperty.PRIMARY_STORAGE_PLAYBOOK_NAME);
        }

        startCleanupThread();

        SurfsGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
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
                return SurfsGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "surfs-primary-storage-image-cleanup-thread";
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
                    bus.send(msg, new CloudBusCallBack(null) {
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
                q.setParameter("ptype", SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE);
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
        return SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String kvmSetupSelfFencerStorageType() {
        return SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE;
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
        completion.fail(errf.stringToOperationError("this has not been supported by surfs"));
    }
    
}
