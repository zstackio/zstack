package org.zstack.storage.ceph.primary;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.vm.VmCapabilities;
import org.zstack.compute.vm.VmCapabilitiesExtensionPoint;
import org.zstack.configuration.DiskOfferingSystemTags;
import org.zstack.configuration.InstanceOfferingSystemTags;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.trash.StorageTrash;
import org.zstack.header.Component;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfigValidator;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfig;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfigValidator;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.progress.TaskProgressRange;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.*;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.primary.KVMCephVolumeTO.MonInfo;
import org.zstack.storage.ceph.primary.capacity.CephPrimaryCapacityUpdater;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.snapshot.MarkRootVolumeAsSnapshotExtension;
import org.zstack.storage.snapshot.PostMarkRootVolumeAsSnapshotExtension;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.getTaskStage;
import static org.zstack.core.progress.ProgressReportService.markTaskStage;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageFactory implements PrimaryStorageFactory, CephCapacityUpdateExtensionPoint, KVMStartVmExtensionPoint,
        KVMAttachVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, CreateTemplateFromVolumeSnapshotExtensionPoint,
        KvmSetupSelfFencerExtensionPoint, KVMPreAttachIsoExtensionPoint, Component, PostMarkRootVolumeAsSnapshotExtension,
        BeforeTakeLiveSnapshotsOnVolumes, VmInstanceCreateExtensionPoint, CreateDataVolumeExtensionPoint,
        InstanceOfferingUserConfigValidator, DiskOfferingUserConfigValidator, MarkRootVolumeAsSnapshotExtension,
        VmCapabilitiesExtensionPoint, PreVmInstantiateResourceExtensionPoint, PSCapacityExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CephPrimaryStorageFactory.class);

    public static final PrimaryStorageType type = new PrimaryStorageType(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);

    {
        type.setSupportSharedVolume(true);
    }

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
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private StorageTrash trash;
    @Autowired
    private EventFacade evtf;

    private Future imageCacheCleanupThread;

    private List<CephPrimaryCapacityUpdater> capacityUpdaters;

    static {
        type.setSupportHeartbeatFile(true);
        type.setOrder(799);
        type.setSupportCreateVolumeSnapshotCheckCapacity(false);
    }

    void init() {
        type.setPrimaryStorageFindBackupStorage(new PrimaryStorageFindBackupStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findBackupStorage(String primaryStorageUuid) {
                List<String> psUuids = new ArrayList<>();
                psUuids.addAll(getExtensionBSUuids(primaryStorageUuid));
                return psUuids;
            }
        });
    }

    private List<String> getExtensionBSUuids(String psUuid) {
        List<String> psUuids = new ArrayList<>();
        List<BackupStoragePrimaryStorageExtensionPoint> extenstions = pluginRgty.getExtensionList(BackupStoragePrimaryStorageExtensionPoint.class);
        extenstions.forEach(ext -> {
            List<String> tmp = ext.getBackupStorageSupportedPS(psUuid);
            if (tmp != null) {
                psUuids.addAll(tmp);
            }
        });
        return psUuids;
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    @Transactional
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        APIAddCephPrimaryStorageMsg cmsg = (APIAddCephPrimaryStorageMsg) msg;
        SystemTagCreator creator;

        CephPrimaryStorageVO cvo = new CephPrimaryStorageVO(vo);
        cvo.setType(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
        cvo.setMountPath(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);

        dbf.getEntityManager().persist(cvo);

        String rootVolumePoolName = cmsg.getRootVolumePoolName() == null ? String.format("pri-v-r-%s", vo.getUuid()) : cmsg.getRootVolumePoolName();
        CephPrimaryStoragePoolVO rootVolumePoolVO = new CephPrimaryStoragePoolVO();
        rootVolumePoolVO.setUuid(Platform.getUuid());
        rootVolumePoolVO.setPrimaryStorageUuid(cvo.getUuid());
        rootVolumePoolVO.setPoolName(rootVolumePoolName);
        rootVolumePoolVO.setType(CephPrimaryStoragePoolType.Root.toString());
        dbf.getEntityManager().persist(rootVolumePoolVO);
        creator = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
        creator.setTagByTokens(map(e(CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN, rootVolumePoolName)));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();

        String dataVolumePoolName = cmsg.getDataVolumePoolName() == null ? String.format("pri-v-d-%s", vo.getUuid()) : cmsg.getDataVolumePoolName();
        CephPrimaryStoragePoolVO dataVolumePoolVO = new CephPrimaryStoragePoolVO();
        dataVolumePoolVO.setUuid(Platform.getUuid());
        dataVolumePoolVO.setPrimaryStorageUuid(cvo.getUuid());
        dataVolumePoolVO.setPoolName(dataVolumePoolName);
        dataVolumePoolVO.setType(CephPrimaryStoragePoolType.Data.toString());
        dbf.getEntityManager().persist(dataVolumePoolVO);
        creator = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
        creator.setTagByTokens(map(e(CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN, dataVolumePoolName)));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();

        String imageCachePoolName = cmsg.getImageCachePoolName() == null ? String.format("pri-c-%s", vo.getUuid()) : cmsg.getImageCachePoolName();
        CephPrimaryStoragePoolVO imageCachePoolVO = new CephPrimaryStoragePoolVO();
        imageCachePoolVO.setUuid(Platform.getUuid());
        imageCachePoolVO.setPrimaryStorageUuid(cvo.getUuid());
        imageCachePoolVO.setPoolName(imageCachePoolName);
        imageCachePoolVO.setType(CephPrimaryStoragePoolType.ImageCache.toString());
        dbf.getEntityManager().persist(imageCachePoolVO);
        creator = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.newSystemTagCreator(cvo.getUuid());
        creator.setTagByTokens(map(e(CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN, imageCachePoolName)));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();

        if (cmsg.getImageCachePoolName() != null) {
            creator = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }
        if (cmsg.getRootVolumePoolName() != null) {
            creator = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }
        if (cmsg.getDataVolumePoolName() != null) {
            creator = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.newSystemTagCreator(cvo.getUuid());
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

        creator = CephSystemTags.KVM_SECRET_UUID.newSystemTagCreator(vo.getUuid());
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
    public void update(CephCapacity cephCapacity) {
        DebugUtils.Assert(cephCapacity.getCephManufacturer() != null,
                "ceph manufacturer is null");

        for (CephPrimaryCapacityUpdater capacityUpdater : capacityUpdaters) {
            if (!cephCapacity.getCephManufacturer().equals(capacityUpdater.getCephManufacturer())) {
                continue;
            }
            capacityUpdater.update(cephCapacity);
            break;
        }
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
                        " and c.imageUuid = :imgUuid and c.installUrl = :path";
                TypedQuery<CephPrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, CephPrimaryStorageVO.class);
                q.setParameter("imgUuid", to.getImageUuid());
                q.setParameter("path", to.getPath());
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

        cto.setSecretUuid(getCephSecretUuid(pri.getUuid()));
        return cto;
    }

    private CdRomTO convertCdRomToCephIfNeeded(final CdRomTO to) {
        if (to == null || to.isEmpty()) {
            return to;
        }

        if (!to.getPath().startsWith(VolumeTO.CEPH)) {
            return to;
        }

        CephPrimaryStorageVO pri = new Callable<CephPrimaryStorageVO>() {
            @Override
            @Transactional(readOnly = true)
            public CephPrimaryStorageVO call() {
                String sql = "select pri from CephPrimaryStorageVO pri, ImageCacheVO c where pri.uuid = c.primaryStorageUuid" +
                        " and c.imageUuid = :imgUuid and c.installUrl = :path";
                TypedQuery<CephPrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, CephPrimaryStorageVO.class);
                q.setParameter("imgUuid", to.getImageUuid());
                q.setParameter("path", to.getPath());
                return q.getSingleResult();
            }
        }.call();

        KvmCephCdRomTO cto = new KvmCephCdRomTO(to);
        cto.setMonInfo(CollectionUtils.transformToList(pri.getMons(), new Function<KvmCephCdRomTO.MonInfo, CephPrimaryStorageMonVO>() {
            @Override
            public KvmCephCdRomTO.MonInfo call(CephPrimaryStorageMonVO arg) {
                if (MonStatus.Connected != arg.getStatus()) {
                    return null;
                }

                KvmCephCdRomTO.MonInfo info = new KvmCephCdRomTO.MonInfo();
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

        cto.setSecretUuid(getCephSecretUuid(pri.getUuid()));
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

        KVMCephVolumeTO cto = new KVMCephVolumeTO(to);
        cto.setSecretUuid(getCephSecretUuid(vol.getPrimaryStorageUuid()));
        cto.setMonInfo(monInfos);
        cto.setDeviceType(VolumeTO.CEPH);
        return cto;
    }

    private String getCephSecretUuid(String psUuid){
        if (CephSystemTags.NO_CEPHX.hasTag(psUuid)){
            return null;
        }

        String secretUuid = CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(psUuid, CephSystemTags.KVM_SECRET_UUID_TOKEN);
        if (secretUuid == null) {
            throw new CloudRuntimeException(String.format("cannot find KVM secret uuid for ceph primary storage[uuid:%s]", psUuid));
        }

        return secretUuid;
    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeToCephIfNeeded(inventory, to);
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, Map data) {
        cmd.setVolume(convertVolumeToCephIfNeeded(volume, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, ErrorCode err, Map data) {

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
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, StartVmCmd cmd) {
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

        List<CdRomTO> cdRomTOS = CollectionUtils.transformToList(cmd.getCdRoms(), new Function<CdRomTO, CdRomTO>() {
            @Override
            public CdRomTO call(CdRomTO arg) {
                return convertCdRomToCephIfNeeded(arg);
            }
        });
        cmd.setCdRoms(cdRomTOS);

        CephPrimaryStorageVO cephPrimaryStorageVO = dbf.findByUuid(spec.getDestRootVolume().getPrimaryStorageUuid(), CephPrimaryStorageVO.class);
        if (cephPrimaryStorageVO != null && !CephSystemTags.NO_CEPHX.hasTag(cephPrimaryStorageVO.getUuid())) {
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

        evtf.onLocal(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (!HostStatus.Disconnected.toString().equals(d.getNewStatus())) {
                    return;
                }

                final String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(
                        d.getHostUuid(), HostSystemTags.EXTRA_IPS_TOKEN);
                if (Strings.isEmpty(extraIps)) {
                    logger.debug(String.format("Host [uuid:%s] has no IPs in data network", d.getHostUuid()));
                    return;
                }

                final List<String> ips = Arrays.stream(extraIps.split(",")).collect(Collectors.toList());

                // if no storage network set for primary storage
                if (d.getInventory().getManagementIp() != null) {
                    ips.add(d.getInventory().getManagementIp());
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        for (String ip: ips) {
                            if (!q(CephPrimaryStorageMonVO.class).eq(CephPrimaryStorageMonVO_.monAddr, ip).isExists()) {
                                continue;
                            }

                            // treat connecting as disconnected
                            // only update connected mon to disconnected status
                            sql(CephPrimaryStorageMonVO.class)
                                    .eq(CephPrimaryStorageMonVO_.monAddr, ip)
                                    .eq(CephPrimaryStorageMonVO_.status, MonStatus.Connected)
                                    .set(CephPrimaryStorageMonVO_.status, MonStatus.Disconnected)
                                    .update();
                            logger.debug(String.format("ceph mon[ip: %s] disconnected, because host[uuid: %s] disconnected", ip, d.getHostUuid()));
                        }
                    }
                }.execute();
            }
        });

        capacityUpdaters = pluginRgty.getExtensionList(CephPrimaryCapacityUpdater.class);

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(final ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();
        final TaskProgressRange CREATE_TEMPORARY_TEMPLATE_STAGE = new TaskProgressRange(0, 10);
        final TaskProgressRange UPLOAD_STAGE = new TaskProgressRange(10, 95);

        final TaskProgressRange parentStage = getTaskStage();
        template.setCreateTemporaryTemplate(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                markTaskStage(parentStage, CREATE_TEMPORARY_TEMPLATE_STAGE);
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
                markTaskStage(parentStage, UPLOAD_STAGE);

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
                msg.setImageUuid(paramIn.getImage().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, paramIn.getPrimaryStorageUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            UploadBitsToBackupStorageReply reply1 = reply.castReply();
                            out.setBackupStorageInstallPath(reply1.getInstallPath() == null? bsInstallPath : reply1.getInstallPath());
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

    @Override
    public void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmMsg otmsg, Map flowData, Completion completion) {
        List<CreateVolumesSnapshotsJobStruct> cephStructs = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct struct : msg.getVolumeSnapshotJobs()) {
            if (Q.New(CephPrimaryStorageVO.class)
                    .eq(CephPrimaryStorageVO_.uuid, struct.getPrimaryStorageUuid())
                    .isExists()) {
                cephStructs.add(struct);
                otmsg.getSnapshotJobs().removeIf(it -> it.getVolumeUuid().equals(struct.getVolumeUuid()));
            }
        }

        if (cephStructs.isEmpty()) {
            completion.success();
            return;
        }

        if (cephStructs.size() == msg.getVolumeSnapshotJobs().size()) {
            flowData.put(VolumeSnapshotConstant.NEED_BLOCK_STREAM_ON_HYPERVISOR, false);
            flowData.put(VolumeSnapshotConstant.NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR, false);
        } else if (msg.getConsistentType() != ConsistentType.None) {
            completion.fail(operr("not support take volumes snapshots " +
                    "on multiple ps when including ceph"));
            return;
        }

        logger.info(String.format("take snapshots for volumes[%s] on %s",
                msg.getLockedVolumeUuids(), getClass().getCanonicalName()));

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(cephStructs).all((struct, whileCompletion) -> {
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

    private Boolean isCephPrimaryStorageVolume(String volumeUuid) {
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
        PrimaryStorageVO primaryStorageVO = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, volumeVO.getPrimaryStorageUuid()).find();

        return primaryStorageVO.getType().equals(type.toString());
    }

    @Override
    public void preCreateVmInstance(CreateVmInstanceMsg msg) {
        settingRootVolume(msg);
        settingDataVolume(msg);
    }

    private void settingRootVolume(CreateVmInstanceMsg msg) {
        String instanceOffering = msg.getInstanceOfferingUuid();

        if (InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.hasTag(instanceOffering)) {
            InstanceOfferingUserConfig config = OfferingUserConfigUtils.getInstanceOfferingConfig(instanceOffering, InstanceOfferingUserConfig.class);
            if (config.getAllocate() != null && config.getAllocate().getPrimaryStorage() != null) {
                msg.setPrimaryStorageUuidForRootVolume(config.getAllocate().getPrimaryStorage().getUuid());

                if (msg.getRootVolumeSystemTags() == null) {
                    msg.setRootVolumeSystemTags(new ArrayList<>());
                }

                if (config.getAllocate().getPrimaryStorage() instanceof CephPrimaryStorageAllocateConfig) {
                    CephPrimaryStorageAllocateConfig primaryStorageAllocateConfig = (CephPrimaryStorageAllocateConfig) config.getAllocate().getPrimaryStorage();
                    if (primaryStorageAllocateConfig.getPoolNames() == null || primaryStorageAllocateConfig.getPoolNames().isEmpty()) {
                        return;
                    }

                    String cephPoolName = SystemTagUtils.findTagValue(msg.getRootVolumeSystemTags(), CephSystemTags.USE_CEPH_ROOT_POOL, CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN);
                    String targetCephPoolName = primaryStorageAllocateConfig.getPoolNames().get(0);
                    if (cephPoolName != null && !cephPoolName.equals(targetCephPoolName)) {
                        throw new OperationFailureException(operr("ceph pool conflict, the ceph pool specified by the instance offering is %s, and the ceph pool specified in the creation parameter is %s"
                                ,targetCephPoolName, cephPoolName));
                    }

                    msg.getRootVolumeSystemTags().add(CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag(
                            map(
                                    e(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN, targetCephPoolName)
                            )
                    ));
                }
            }
        }

        String rootDiskOffering = msg.getRootDiskOfferingUuid();
        if (rootDiskOffering == null) {
            return;
        }

        if (!DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(rootDiskOffering)) {
            return;
        }

        DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(rootDiskOffering, DiskOfferingUserConfig.class);
        if (config.getAllocate() == null) {
            return;
        }

        if (config.getAllocate().getPrimaryStorage() == null) {
            return;
        }

        msg.setPrimaryStorageUuidForRootVolume(config.getAllocate().getPrimaryStorage().getUuid());

        if (msg.getRootVolumeSystemTags() == null) {
            msg.setRootVolumeSystemTags(new ArrayList<>());
        }

        if (!(config.getAllocate().getPrimaryStorage() instanceof CephPrimaryStorageAllocateConfig)) {
            return;
        }

        CephPrimaryStorageAllocateConfig primaryStorageAllocateConfig = (CephPrimaryStorageAllocateConfig) config.getAllocate().getPrimaryStorage();
        if (primaryStorageAllocateConfig.getPoolNames() == null || primaryStorageAllocateConfig.getPoolNames().isEmpty()) {
            return;
        }

        String cephPoolName = SystemTagUtils.findTagValue(msg.getRootVolumeSystemTags(), CephSystemTags.USE_CEPH_ROOT_POOL, CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN);
        String targetCephPoolName = primaryStorageAllocateConfig.getPoolNames().get(0);
        if (cephPoolName != null && !cephPoolName.equals(targetCephPoolName)) {
            throw new OperationFailureException(operr("ceph pool conflict, the ceph pool specified by the disk offering is %s, and the ceph pool specified in the creation parameter is %s"
                    ,targetCephPoolName, cephPoolName));
        }

        msg.getRootVolumeSystemTags().add(CephSystemTags.USE_CEPH_ROOT_POOL.instantiateTag(
                map(
                        e(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN, targetCephPoolName)
                )
        ));
    }

    private void settingDataVolume(CreateVmInstanceMsg msg) {
        if (msg.getDataDiskOfferingUuids() == null || msg.getDataDiskOfferingUuids().isEmpty()) {
            return;
        }

        String diskOffering = msg.getDataDiskOfferingUuids().get(0);
        if (diskOffering == null) {
            return;
        }

        if (DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, DiskOfferingUserConfig.class);

            if (config.getAllocate() == null) {
                return;
            }

            if (config.getAllocate().getPrimaryStorage() != null) {
                msg.setPrimaryStorageUuidForDataVolume(config.getAllocate().getPrimaryStorage().getUuid());
            }

            if (msg.getDataVolumeSystemTags() == null) {
                msg.setDataVolumeSystemTags(new ArrayList<>());
            }

            if (config.getAllocate().getPrimaryStorage() instanceof CephPrimaryStorageAllocateConfig) {
                CephPrimaryStorageAllocateConfig primaryStorageAllocateConfig = (CephPrimaryStorageAllocateConfig) config.getAllocate().getPrimaryStorage();
                msg.getDataVolumeSystemTags().add(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.instantiateTag(
                        map(
                                e(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN, primaryStorageAllocateConfig.getPoolNames().get(0))
                        )
                ));
            }
        }
    }

    @Override
    public void preCreateVolume(APICreateDataVolumeMsg msg) {
        String diskOffering = msg.getDiskOfferingUuid();
        if (diskOffering == null) {
            return;
        }

        if (DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, DiskOfferingUserConfig.class);

            if (config.getAllocate() == null) {
                return;
            }

            if (config.getAllocate().getPrimaryStorage() == null) {
                return;
            }
            msg.setPrimaryStorageUuid(config.getAllocate().getPrimaryStorage().getUuid());
            if (!(config.getAllocate().getPrimaryStorage() instanceof CephPrimaryStorageAllocateConfig)) {
                return;
            }

            CephPrimaryStorageAllocateConfig primaryStorageAllocateConfig = (CephPrimaryStorageAllocateConfig) config.getAllocate().getPrimaryStorage();
            if (primaryStorageAllocateConfig.getPoolNames() == null || primaryStorageAllocateConfig.getPoolNames().isEmpty()) {
                return;
            }

            String cephPoolName = SystemTagUtils.findTagValue(msg.getSystemTags(), CephSystemTags.USE_CEPH_ROOT_POOL, CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN);
            String targetCephPoolName = primaryStorageAllocateConfig.getPoolNames().get(0);
            if (cephPoolName != null && !cephPoolName.equals(targetCephPoolName)) {
                throw new OperationFailureException(operr("ceph pool conflict, the ceph pool specified by the disk offering is %s, and the ceph pool specified in the creation parameter is %s"
                        ,targetCephPoolName, cephPoolName));
            }

            msg.addSystemTag(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.instantiateTag(
                    map(
                            e(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN, primaryStorageAllocateConfig.getPoolNames().get(0))
                    )
            ));
        }
    }

    @Override
    public void afterCreateVolume(VolumeVO vo) {
        return;
    }

    @Override
    public void beforeCreateVolume(VolumeInventory volume) {
        return;
    }

    @Override
    public void validateInstanceOfferingUserConfig(String userConfig, String instanceOfferingUuid) {
        if (StringUtils.isBlank(userConfig)) {
            return;
        }

        InstanceOfferingUserConfig config;

        try {
            config = OfferingUserConfigUtils.toObject(userConfig, InstanceOfferingUserConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Syntax error(s) in billing instance offering user configuration, user configuration should write in json format.", e);
        }

        if (config.getAllocate() == null) {
            return;
        }

        PrimaryStorageAllocateConfig primaryStorageAllocateConfig = config.getAllocate().getPrimaryStorage();
        if (primaryStorageAllocateConfig == null) {
            return;
        }

        if (primaryStorageAllocateConfig.getType() == null) {
            throw new IllegalArgumentException("primaryStorage type cannot be empty");
        }

        if (primaryStorageAllocateConfig.getUuid() == null) {
            throw new IllegalArgumentException("primaryStorage uuid cannot be empty");
        }

        String psUuid = primaryStorageAllocateConfig.getUuid();
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        if (primaryStorageVO == null) {
            throw new IllegalArgumentException(String.format("primaryStorage[uuid=%s] does not exist", psUuid));
        }

        if (!(config.getAllocate().getPrimaryStorage() instanceof CephPrimaryStorageAllocateConfig)) {
            return;
        }

        CephPrimaryStorageAllocateConfig cephAllocateConfig = (CephPrimaryStorageAllocateConfig) config.getAllocate().getPrimaryStorage();
        if (cephAllocateConfig.getPoolNames() == null || cephAllocateConfig.getPoolNames().isEmpty()) {
            return;
        }

        for (String poolName : cephAllocateConfig.getPoolNames()) {
            boolean exists = Q.New(CephPrimaryStoragePoolVO.class)
                    .eq(CephPrimaryStoragePoolVO_.poolName, poolName)
                    .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, psUuid)
                    .isExists();
            if (!exists) {
                throw new IllegalArgumentException(String.format("cephPrimaryStorage[uuid=%s] cephPool[name=%s] does not exist", psUuid, poolName));
            }
        }
    }

    @Override
    public void validateDiskOfferingUserConfig(String userConfig, String diskOfferingUuid) {
        if (StringUtils.isBlank(userConfig)) {
            return;
        }

        DiskOfferingUserConfig config;

        try {
            config = OfferingUserConfigUtils.toObject(userConfig, DiskOfferingUserConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Syntax error(s) in disk offering user configuration, user configuration should write in json format.", e);
        }

        if (config.getAllocate() == null) {
            return;
        }

        PrimaryStorageAllocateConfig primaryStorageAllocateConfig = config.getAllocate().getPrimaryStorage();
        if (primaryStorageAllocateConfig == null) {
            return;
        }

        if (primaryStorageAllocateConfig.getType() == null) {
            throw new IllegalArgumentException("primaryStorage type cannot be empty");
        }

        if (primaryStorageAllocateConfig.getUuid() == null) {
            throw new IllegalArgumentException("primaryStorage uuid cannot be empty");
        }

        String psUuid = primaryStorageAllocateConfig.getUuid();
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        if (primaryStorageVO == null) {
            throw new IllegalArgumentException(String.format("primaryStorage[uuid=%s] does not exist", psUuid));
        }

        if (!(config.getAllocate().getPrimaryStorage() instanceof CephPrimaryStorageAllocateConfig)) {
            return;
        }

        CephPrimaryStorageAllocateConfig cephAllocateConfig = (CephPrimaryStorageAllocateConfig) config.getAllocate().getPrimaryStorage();
        if (cephAllocateConfig.getPoolNames() == null || cephAllocateConfig.getPoolNames().isEmpty()) {
            return;
        }

        for (String poolName : cephAllocateConfig.getPoolNames()) {
            boolean exists = Q.New(CephPrimaryStoragePoolVO.class)
                    .eq(CephPrimaryStoragePoolVO_.poolName, poolName)
                    .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, psUuid)
                    .isExists();
            if (!exists) {
                throw new IllegalArgumentException(String.format("cephPrimaryStorage[uuid=%s] cephPool[name=%s] does not exist", psUuid, poolName));
            }
        }
    }

    @Override
    public List<Flow> markRootVolumeAsSnapshot(VolumeInventory vol, VolumeSnapshotVO vo, String accountUuid) {
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
                        vo.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                        trigger.next();
                    }
                });
            }
        });
        return flows;
    }

    @Override
    public String getExtensionPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void checkVmCapability(VmInstanceInventory inv, VmCapabilities capabilities) {
        capabilities.setSupportMemorySnapshot(false);
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
        // do nothing
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (VmInstanceConstant.VmOperation.Start != spec.getCurrentVmOperation()) {
            completion.success();
            return;
        }

        VolumeInventory rootVolume = spec.getDestRootVolume();
        if (rootVolume == null) {
            completion.success();
            return;
        }

        boolean flag = CephGlobalConfig.PREVENT_VM_SPLIT_BRAIN.value(Boolean.class);
        if (!flag) {
            completion.success();
            return;
        }

        boolean isCeph = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, rootVolume.getPrimaryStorageUuid())
                .eq(PrimaryStorageVO_.type, CephConstants.CEPH_PRIMARY_STORAGE_TYPE)
                .isExists();
        if (!isCeph) {
            completion.success();
            return;
        }

        GetVolumeWatchersMsg msg = new GetVolumeWatchersMsg();
        msg.setPrimaryStorageUuid(rootVolume.getPrimaryStorageUuid());
        msg.setVolumeUuid(rootVolume.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());

        List<String> avoidHostUuids = new ArrayList<>();
        if (spec.getAvoidHostUuids() != null) {
            avoidHostUuids.addAll(spec.getAvoidHostUuids());
        }
        if (spec.getSoftAvoidHostUuids() != null ) {
            avoidHostUuids.addAll(spec.getSoftAvoidHostUuids());
        }

        if (!avoidHostUuids.isEmpty()) {
            List<String> hostIps = Q.New(HostVO.class)
                    .in(HostVO_.uuid, avoidHostUuids)
                    .select(HostVO_.managementIp)
                    .listValues();

            if (!hostIps.isEmpty()) {
                List<String> monUuids = Q.New(CephPrimaryStorageMonVO.class)
                        .select(CephPrimaryStorageMonVO_.uuid)
                        .eq(CephPrimaryStorageMonVO_.primaryStorageUuid, rootVolume.getPrimaryStorageUuid())
                        .in(CephPrimaryStorageMonVO_.hostname, hostIps)
                        .listValues();
                msg.setAvoidCephMonUuids(monUuids);
            }
        }

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("get rootVolume[%s] rbd image watchers fail, %s",
                            rootVolume.getInstallPath(), reply.getError().getDetails()));
                    return;
                }

                GetVolumeWatchersReply rly = (GetVolumeWatchersReply)reply;
                List watchers = rly.getWatchers();
                if (watchers == null || watchers.isEmpty()) {
                    completion.success();
                    return;
                }

                String installPath = Q.New(VolumeVO.class)
                        .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                        .select(VolumeVO_.installPath)
                        .findValue();
                completion.fail(operr("rootVolume[%s] is already in use(ceph rbd image[%s] already has watchers), in order to prevent brain splitting, Starting VM is prohibited.",
                    msg.getVolumeUuid(), installPath));
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv) {
        return Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url).eq(PrimaryStorageVO_.uuid, psInv.getUuid()).findValue();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long reserveCapacity(AllocatePrimaryStorageSpaceMsg msg, String allocatedInstallUrl, long size, String psUuid) {
        return size;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseCapacity(String allocatedInstallUrl, long size, String psUuid) {
    }
}
