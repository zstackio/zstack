package org.zstack.compute.vm;

import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.*;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.scheduler.SchedulerConstant;
import org.zstack.core.scheduler.SchedulerFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.AllocateHostDryRunReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.DiskOfferingVO_;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.search.SearchOp;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.volume.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.list;

public class VmInstanceManagerImpl extends AbstractService implements
        VmInstanceManager,
        ReportQuotaExtensionPoint,
        ManagementNodeReadyExtensionPoint,
        L3NetworkDeleteExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint,
        GlobalApiMessageInterceptor,
        PrimaryStorageDeleteExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmInstanceManagerImpl.class);
    private Map<String, VmInstanceFactory> vmInstanceFactories = Collections.synchronizedMap(new HashMap<>());
    private List<String> createVmWorkFlowElements;
    private List<String> stopVmWorkFlowElements;
    private List<String> rebootVmWorkFlowElements;
    private List<String> startVmWorkFlowElements;
    private List<String> destroyVmWorkFlowElements;
    private List<String> migrateVmWorkFlowElements;
    private List<String> attachIsoWorkFlowElements;
    private List<String> detachIsoWorkFlowElements;
    private List<String> attachVolumeWorkFlowElements;
    private List<String> expungeVmWorkFlowElements;
    private List<String> suspendVmWorkFlowElements;
    private List<String> resumeVmWorkFlowElements;
    private FlowChainBuilder createVmFlowBuilder;
    private FlowChainBuilder stopVmFlowBuilder;
    private FlowChainBuilder rebootVmFlowBuilder;
    private FlowChainBuilder startVmFlowBuilder;
    private FlowChainBuilder destroyVmFlowBuilder;
    private FlowChainBuilder migrateVmFlowBuilder;
    private FlowChainBuilder attachVolumeFlowBuilder;
    private FlowChainBuilder attachIsoFlowBuilder;
    private FlowChainBuilder detachIsoFlowBuilder;
    private FlowChainBuilder expungeVmFlowBuilder;
    private FlowChainBuilder suspendVmFlowBuilder;
    private FlowChainBuilder resumeVmFlowBuilder;
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<>();
    private Future<Void> expungeVmTask;
    private Map<Class, VmInstanceBaseExtensionFactory> vmInstanceBaseExtensionFactories = new HashMap<>();

    static {
        allowedMessageAfterSoftDeletion.add(VmInstanceDeletionMsg.class);
    }

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private GCFacade gcf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private VmInstanceDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private HostAllocatorManager hostAllocatorMgr;
    @Autowired
    private SchedulerFacade schedulerFacade;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    void passThrough(VmInstanceMessage msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            VmInstanceEO eo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceEO.class);
            vo = ObjectUtils.newAndCopy(eo, VmInstanceVO.class);
        }

        if (vo == null) {
            String err = String.format("Cannot find VmInstance[uuid:%s], it may have been deleted", msg.getVmInstanceUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        VmInstanceFactory factory = getVmInstanceFactory(VmInstanceType.valueOf(vo.getType()));
        VmInstance vm = factory.getVmInstance(vo);
        vm.handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateVmInstanceMsg) {
            handle((CreateVmInstanceMsg) msg);
        } else if (msg instanceof VmInstanceMessage) {
            passThrough((VmInstanceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVmInstanceMsg) {
            handle((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIListVmInstanceMsg) {
            handle((APIListVmInstanceMsg) msg);
        } else if (msg instanceof APISearchVmInstanceMsg) {
            handle((APISearchVmInstanceMsg) msg);
        } else if (msg instanceof APIGetVmInstanceMsg) {
            handle((APIGetVmInstanceMsg) msg);
        } else if (msg instanceof APIListVmNicMsg) {
            handle((APIListVmNicMsg) msg);
        } else if (msg instanceof APIGetCandidateZonesClustersHostsForCreatingVmMsg) {
            handle((APIGetCandidateZonesClustersHostsForCreatingVmMsg) msg);
        } else if (msg instanceof APIGetInterdependentL3NetworksImagesMsg) {
            handle((APIGetInterdependentL3NetworksImagesMsg) msg);
        } else if (msg instanceof APIGetCandidateVmForAttachingIsoMsg) {
            handle((APIGetCandidateVmForAttachingIsoMsg) msg);
        } else if (msg instanceof VmInstanceMessage) {
            passThrough((VmInstanceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private void handle(APIGetCandidateVmForAttachingIsoMsg msg) {
        APIGetCandidateVmForAttachingIsoReply reply = new APIGetCandidateVmForAttachingIsoReply();

        String sql = "select bs" +
                " from BackupStorageVO bs, ImageBackupStorageRefVO ref" +
                " where ref.imageUuid = :isoUuid" +
                " and bs.uuid = ref.backupStorageUuid";
        TypedQuery<BackupStorageVO> q = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
        q.setParameter("isoUuid", msg.getIsoUuid());
        List<BackupStorageVO> bss = q.getResultList();
        if (bss.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<String> psUuids = new ArrayList<>();
        List<String> psTypes = new ArrayList<>();
        for (BackupStorageVO bs : bss) {
            BackupStorageType bsType = BackupStorageType.valueOf(bs.getType());
            List<String> lst = bsType.findRelatedPrimaryStorage(bs.getUuid());
            if (lst != null) {
                psUuids.addAll(lst);
            } else {
                psTypes.addAll(hostAllocatorMgr.getPrimaryStorageTypesByBackupStorageTypeFromMetrics(bs.getType()));
            }
        }

        List<VmInstanceVO> vms = new ArrayList<>();
        if (!psUuids.isEmpty()) {
            sql = "select vm" +
                    " from VmInstanceVO vm, VolumeVO vol" +
                    " where vol.type = :volType" +
                    " and vol.vmInstanceUuid = vm.uuid" +
                    " and vm.state in (:vmStates)" +
                    " and vol.primaryStorageUuid in (:psUuids)";
            TypedQuery<VmInstanceVO> vmq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vmq.setParameter("volType", VolumeType.Root);
            vmq.setParameter("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped));
            vmq.setParameter("psUuids", psUuids);
            vms.addAll(vmq.getResultList());
        }

        if (!psTypes.isEmpty()) {
            sql = "select vm" +
                    " from VmInstanceVO vm, VolumeVO vol, PrimaryStorageVO ps" +
                    " where vol.type = :volType" +
                    " and vol.vmInstanceUuid = vm.uuid" +
                    " and vm.state in (:vmStates)" +
                    " and vol.primaryStorageUuid = ps.uuid" +
                    " and ps.type in (:psTypes)";
            TypedQuery<VmInstanceVO> vmq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vmq.setParameter("volType", VolumeType.Root);
            vmq.setParameter("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped));
            vmq.setParameter("psTypes", psTypes);
            vms.addAll(vmq.getResultList());
        }

        reply.setInventories(VmInstanceInventory.valueOf(vms));
        bus.reply(msg, reply);
    }

    private void handle(APIGetInterdependentL3NetworksImagesMsg msg) {
        if (msg.getImageUuid() != null) {
            getInterdependentL3NetworksByImageUuid(msg);
        } else {
            getInterdependentImagesByL3NetworkUuids(msg);
        }
    }

    private List<BackupStorageVO> listIntersection(List<BackupStorageVO> a, List<BackupStorageVO> b) {
        List<BackupStorageVO> ret = new ArrayList<>();
        for (BackupStorageVO s : a) {
            if (b.stream().filter(it -> it.getUuid().equals(s.getUuid())).findAny().isPresent()) {
                ret.add(s);
            }
        }

        return ret;
    }

    @Transactional(readOnly = true)
    private void getInterdependentImagesByL3NetworkUuids(APIGetInterdependentL3NetworksImagesMsg msg) {
        APIGetInterdependentL3NetworkImageReply reply = new APIGetInterdependentL3NetworkImageReply();

        List<List<BackupStorageVO>> bss = new ArrayList<>();
        for (String l3uuid : msg.getL3NetworkUuids()) {
            String sql = "select ps" +
                    " from PrimaryStorageVO ps, L2NetworkClusterRefVO l2ref," +
                    " L3NetworkVO l3, PrimaryStorageClusterRefVO psref" +
                    " where ps.uuid = psref.primaryStorageUuid" +
                    " and psref.clusterUuid = l2ref.clusterUuid" +
                    " and l2ref.l2NetworkUuid = l3.l2NetworkUuid" +
                    " and l3.uuid = :l3uuid";
            TypedQuery<PrimaryStorageVO> psq = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            psq.setParameter("l3uuid", l3uuid);
            List<PrimaryStorageVO> pss = psq.getResultList();

            List<BackupStorageVO> lst = new ArrayList<>();
            for (PrimaryStorageVO ps : pss) {
                PrimaryStorageType psType = PrimaryStorageType.valueOf(ps.getType());
                List<String> bsUuids = psType.findBackupStorage(ps.getUuid());

                if (bsUuids == null) {
                    // the primary storage doesn't have bound backup storage
                    sql = "select bs from BackupStorageVO bs where bs.type in (:types)";
                    TypedQuery<BackupStorageVO> bq = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
                    bq.setParameter("types", hostAllocatorMgr.getBackupStorageTypesByPrimaryStorageTypeFromMetrics(ps.getType()));
                    lst.addAll(bq.getResultList());
                } else if (!bsUuids.isEmpty()) {
                    // the primary storage has bound backup storage, e.g. ceph, fusionstor
                    sql = "select bs from BackupStorageVO bs where bs.uuid in (:uuids)";
                    TypedQuery<BackupStorageVO> bq = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
                    bq.setParameter("uuids", bsUuids);
                    lst.addAll(bq.getResultList());
                } else {
                    logger.warn(String.format("the primary storage[uuid:%s, type:%s] needs a bound backup storage," +
                            " but seems it's not added", ps.getUuid(), ps.getType()));
                }
            }

            bss.add(lst);
        }

        List<BackupStorageVO> selectedBss = new ArrayList<>();
        for (List<BackupStorageVO> lst : bss) {
            selectedBss.addAll(lst);
        }

        for (List<BackupStorageVO> l : bss) {
            selectedBss = listIntersection(selectedBss, l);
        }

        if (selectedBss.isEmpty()) {
            reply.setInventories(new ArrayList());
            bus.reply(msg, reply);
            return;
        }

        List<String> bsUuids = selectedBss.stream().map(BackupStorageVO::getUuid).collect(Collectors.toList());
        String sql = "select img" +
                " from ImageVO img, ImageBackupStorageRefVO iref, BackupStorageZoneRefVO zref, BackupStorageVO bs" +
                " where img.uuid = iref.imageUuid" +
                " and iref.backupStorageUuid = zref.backupStorageUuid" +
                " and bs.uuid = zref.backupStorageUuid" +
                " and bs.uuid in (:uuids)" +
                " and zref.zoneUuid = :zoneUuid" +
                " group by img.uuid";
        TypedQuery<ImageVO> iq = dbf.getEntityManager().createQuery(sql, ImageVO.class);
        iq.setParameter("uuids", bsUuids);
        iq.setParameter("zoneUuid", msg.getZoneUuid());
        List<ImageVO> vos = iq.getResultList();
        reply.setInventories(ImageInventory.valueOf(vos));
        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    private void getInterdependentL3NetworksByImageUuid(APIGetInterdependentL3NetworksImagesMsg msg) {
        APIGetInterdependentL3NetworkImageReply reply = new APIGetInterdependentL3NetworkImageReply();

        String sql = "select bs" +
                " from BackupStorageVO bs, ImageBackupStorageRefVO ref, BackupStorageZoneRefVO zref" +
                " where bs.uuid = ref.backupStorageUuid" +
                " and ref.imageUuid = :imgUuid" +
                " and ref.backupStorageUuid = zref.backupStorageUuid" +
                " and zref.zoneUuid = :zoneUuid";
        TypedQuery<BackupStorageVO> bsq = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
        bsq.setParameter("imgUuid", msg.getImageUuid());
        bsq.setParameter("zoneUuid", msg.getZoneUuid());
        List<BackupStorageVO> bss = bsq.getResultList();
        if (bss.isEmpty()) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                    String.format("the image[uuid:%s] is not on any backup storage that has been attached to the zone[uuid:%s]",
                            msg.getImageUuid(), msg.getZoneUuid())
            ));
        }

        List<L3NetworkVO> l3s = new ArrayList<>();
        for (BackupStorageVO bs : bss) {
            BackupStorageType bsType = BackupStorageType.valueOf(bs.getType());
            List<String> relatedPrimaryStorageUuids = bsType.findRelatedPrimaryStorage(bs.getUuid());
            if (relatedPrimaryStorageUuids == null) {
                // the backup storage has no strongly-bound primary storage
                List<String> psTypes = hostAllocatorMgr.getPrimaryStorageTypesByBackupStorageTypeFromMetrics(bs.getType());
                sql = "select l3" +
                        " from L3NetworkVO l3, L2NetworkClusterRefVO l2ref," +
                        " PrimaryStorageClusterRefVO psref, PrimaryStorageVO ps" +
                        " where l3.l2NetworkUuid = l2ref.l2NetworkUuid" +
                        " and l2ref.clusterUuid = psref.clusterUuid" +
                        " and psref.primaryStorageUuid = ps.uuid" +
                        " and ps.type in (:psTypes)" +
                        " and ps.zoneUuid = l3.zoneUuid" +
                        " and l3.zoneUuid = :zoneUuid" +
                        " group by l3.uuid";
                TypedQuery<L3NetworkVO> l3q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                l3q.setParameter("psTypes", psTypes);
                l3q.setParameter("zoneUuid", msg.getZoneUuid());
                l3s.addAll(l3q.getResultList());
            } else if (!relatedPrimaryStorageUuids.isEmpty()) {
                // the backup storage has strongly-bound primary storage, e.g. ceph, fusionstor
                sql = "select l3" +
                        " from L3NetworkVO l3, L2NetworkClusterRefVO l2ref," +
                        " PrimaryStorageClusterRefVO psref, PrimaryStorageVO ps" +
                        " where l3.l2NetworkUuid = l2ref.l2NetworkUuid" +
                        " and l2ref.clusterUuid = psref.clusterUuid" +
                        " and psref.primaryStorageUuid = ps.uuid" +
                        " and ps.uuid in (:psUuids)" +
                        " and ps.zoneUuid = l3.zoneUuid" +
                        " and l3.zoneUuid = :zoneUuid" +
                        " group by l3.uuid";
                TypedQuery<L3NetworkVO> l3q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
                l3q.setParameter("psUuids", relatedPrimaryStorageUuids);
                l3q.setParameter("zoneUuid", msg.getZoneUuid());
                l3s.addAll(l3q.getResultList());
            } else {
                logger.warn(String.format("the backup storage[uuid:%s, type: %s] needs a strongly-bound primary storage," +
                        " but seems the primary storage is not added", bs.getUuid(), bs.getType()));
            }
        }


        reply.setInventories(L3NetworkInventory.valueOf(l3s));
        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateZonesClustersHostsForCreatingVmMsg msg) {
        DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();

        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (image.getMediaType() == ImageMediaType.ISO && msg.getRootDiskOfferingUuid() == null) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                    String.format("the image[name:%s, uuid:%s] is an ISO, rootDiskOfferingUuid must be set",
                            image.getName(), image.getUuid())
            ));
        }

        amsg.setImage(ImageInventory.valueOf(image));
        amsg.setZoneUuid(msg.getZoneUuid());
        amsg.setClusterUuid(msg.getClusterUuid());

        InstanceOfferingVO insvo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        amsg.setCpuCapacity(insvo.getCpuNum());
        amsg.setMemoryCapacity(insvo.getMemorySize());

        long diskSize = 0;
        List<DiskOfferingInventory> diskOfferings = new ArrayList<>();
        if (msg.getDataDiskOfferingUuids() != null) {
            SimpleQuery<DiskOfferingVO> q = dbf.createQuery(DiskOfferingVO.class);
            q.add(DiskOfferingVO_.uuid, Op.IN, msg.getDataDiskOfferingUuids());
            List<DiskOfferingVO> dvos = q.list();
            diskOfferings.addAll(DiskOfferingInventory.valueOf(dvos));
        }

        if (image.getMediaType() == ImageMediaType.ISO) {
            DiskOfferingVO rootDiskOffering = dbf.findByUuid(msg.getRootDiskOfferingUuid(), DiskOfferingVO.class);
            diskOfferings.add(DiskOfferingInventory.valueOf(rootDiskOffering));
        } else {
            diskSize = image.getSize();
        }

        diskSize += diskOfferings.stream().mapToLong(DiskOfferingInventory::getDiskSize).sum();
        amsg.setDiskSize(diskSize);
        amsg.setL3NetworkUuids(msg.getL3NetworkUuids());
        amsg.setVmOperation(VmOperation.NewCreate.toString());
        amsg.setDryRun(true);
        amsg.setListAllHosts(true);
        amsg.setAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);

        if (image.getBackupStorageRefs().size() == 1) {
            amsg.setRequiredBackupStorageUuid(image.getBackupStorageRefs().iterator().next().getBackupStorageUuid());
        } else {
            if (msg.getZoneUuid() == null) {
                throw new OperationFailureException(errf.stringToInvalidArgumentError(
                        String.format("zoneUuid must be set because the image[name:%s, uuid:%s] is on multiple backup storage",
                                image.getName(), image.getUuid())
                ));
            }

            ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
            selector.setZoneUuid(msg.getZoneUuid());
            selector.setImageUuid(image.getUuid());
            amsg.setRequiredBackupStorageUuid(selector.select());
        }

        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setUuid(Platform.FAKE_UUID);
        vm.setInstanceOfferingUuid(insvo.getUuid());
        vm.setImageUuid(image.getUuid());
        vm.setCpuNum(insvo.getCpuNum());
        vm.setMemorySize(insvo.getMemorySize());
        vm.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid() == null ? msg.getL3NetworkUuids().get(0) : msg.getDefaultL3NetworkUuid());
        vm.setName("for-getting-candidates-zones-clusters-hosts");
        amsg.setVmInstance(vm);

        APIGetCandidateZonesClustersHostsForCreatingVmReply areply = new APIGetCandidateZonesClustersHostsForCreatingVmReply();
        bus.makeLocalServiceId(amsg, HostAllocatorConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    areply.setError(reply.getError());
                } else {
                    AllocateHostDryRunReply re = reply.castReply();

                    if (!re.getHosts().isEmpty()) {
                        areply.setHosts(re.getHosts());

                        List<String> clusterUuids = re.getHosts().stream().
                                map(HostInventory::getClusterUuid).collect(Collectors.toList());
                        areply.setClusters(ClusterInventory.valueOf(dbf.listByPrimaryKeys(clusterUuids, ClusterVO.class)));

                        Map<String, List<PrimaryStorageInventory>> m = new HashMap<>();
                        for (String clusterUuid : clusterUuids) {
                            SimpleQuery<PrimaryStorageClusterRefVO> sq = dbf.createQuery(PrimaryStorageClusterRefVO.class);
                            sq.add(PrimaryStorageClusterRefVO_.clusterUuid, Op.EQ, clusterUuid);
                            List<PrimaryStorageClusterRefVO> refs = sq.list();
                            if (refs != null && !refs.isEmpty()) {
                                List<PrimaryStorageInventory> l = refs.stream().map(
                                        ref -> PrimaryStorageInventory.valueOf(
                                                dbf.findByUuid(ref.getPrimaryStorageUuid(), PrimaryStorageVO.class))
                                ).collect(Collectors.toList());
                                m.put(clusterUuid, l);
                            } else {
                                m.put(clusterUuid, new ArrayList<>());
                            }
                        }
                        areply.setClusterPsMap(m);

                        List<String> zoneUuids = re.getHosts().stream().
                                map(HostInventory::getZoneUuid).collect(Collectors.toList());
                        areply.setZones(ZoneInventory.valueOf(dbf.listByPrimaryKeys(zoneUuids, ZoneVO.class)));
                    } else {
                        areply.setHosts(new ArrayList<>());
                        areply.setClusters(new ArrayList<>());
                        areply.setZones(new ArrayList<>());
                        areply.setClusterPsMap(new HashMap<>());
                    }
                }

                bus.reply(msg, areply);
            }
        });
    }

    private void handle(APIListVmNicMsg msg) {
        List<VmNicVO> vos = dbf.listByApiMessage(msg, VmNicVO.class);
        List<VmNicInventory> invs = VmNicInventory.valueOf(vos);
        APIListVmNicReply reply = new APIListVmNicReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIGetVmInstanceMsg msg) {
        SearchQuery<VmInstanceInventory> query = new SearchQuery(VmInstanceInventory.class);
        query.addAccountAsAnd(msg);
        query.add("uuid", SearchOp.AND_EQ, msg.getUuid());
        List<VmInstanceInventory> invs = query.list();
        APIGetVmInstanceReply reply = new APIGetVmInstanceReply();
        if (!invs.isEmpty()) {
            reply.setInventory(JSONObjectUtil.toJsonString(invs.get(0)));
        }
        bus.reply(msg, reply);
    }

    private void handle(APISearchVmInstanceMsg msg) {
        SearchQuery<VmInstanceInventory> query = SearchQuery.create(msg, VmInstanceInventory.class);
        query.addAccountAsAnd(msg);
        String content = query.listAsString();
        APISearchVmInstanceReply reply = new APISearchVmInstanceReply();
        reply.setContent(content);
        bus.reply(msg, reply);
    }

    private void handle(APIListVmInstanceMsg msg) {
        List<VmInstanceVO> vos = dl.listByApiMessage(msg, VmInstanceVO.class);
        List<VmInstanceInventory> invs = VmInstanceInventory.valueOf(vos);
        APIListVmInstanceReply reply = new APIListVmInstanceReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void doCreateVmInstance(final CreateVmInstanceMsg msg, final APICreateMessage cmsg, ReturnValueCompletion<VmInstanceInventory> completion) {
        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();
        VmInstanceVO vo = new VmInstanceVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setClusterUuid(msg.getClusterUuid());
        vo.setDescription(msg.getDescription());
        vo.setHostUuid(msg.getHostUuid());
        vo.setImageUuid(msg.getImageUuid());
        vo.setInstanceOfferingUuid(instanceOfferingUuid);
        vo.setState(VmInstanceState.Created);
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setInternalId(dbf.generateSequenceNumber(VmInstanceSequenceNumberVO.class));
        vo.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());

        SimpleQuery<ImageVO> imgq = dbf.createQuery(ImageVO.class);
        imgq.select(ImageVO_.platform);
        imgq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        ImagePlatform platform = imgq.findValue();
        vo.setPlatform(platform.toString());

        vo.setCpuNum(msg.getCpuNum());
        vo.setCpuSpeed(msg.getCpuSpeed());
        vo.setMemorySize(msg.getMemorySize());
        vo.setAllocatorStrategy(msg.getAllocatorStrategy());

        acntMgr.createAccountResourceRef(msg.getAccountUuid(), vo.getUuid(), VmInstanceVO.class);

        String vmType = msg.getType() == null ? VmInstanceConstant.USER_VM_TYPE : msg.getType();
        VmInstanceType type = VmInstanceType.valueOf(vmType);
        VmInstanceFactory factory = getVmInstanceFactory(type);
        vo = factory.createVmInstance(vo, msg);

        if (cmsg != null) {
            tagMgr.createTagsFromAPICreateMessage(cmsg, vo.getUuid(), VmInstanceVO.class.getSimpleName());
        }

        if (instanceOfferingUuid != null) {
            tagMgr.copySystemTag(
                    instanceOfferingUuid,
                    InstanceOfferingVO.class.getSimpleName(),
                    vo.getUuid(),
                    VmInstanceVO.class.getSimpleName());
        }

        if (msg.getImageUuid() != null) {
            tagMgr.copySystemTag(
                    msg.getImageUuid(),
                    ImageVO.class.getSimpleName(),
                    vo.getUuid(),
                    VmInstanceVO.class.getSimpleName());
        }

        if (VmCreationStrategy.JustCreate == VmCreationStrategy.valueOf(msg.getStrategy())) {
            VmInstanceInventory inv = VmInstanceInventory.valueOf(vo);
            createVmButNotStart(msg, inv);
            completion.success(inv);
            return;
        }

        StartNewCreatedVmInstanceMsg smsg = new StartNewCreatedVmInstanceMsg();
        smsg.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        smsg.setL3NetworkUuids(msg.getL3NetworkUuids());
        smsg.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        smsg.setVmInstanceInventory(VmInstanceInventory.valueOf(vo));
        smsg.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        smsg.setRootPassword(msg.getRootPassword());
        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, vo.getUuid());
        bus.send(smsg, new CloudBusCallBack(smsg) {
            @Override
            public void run(MessageReply reply) {
                try {
                    if (reply.isSuccess()) {
                        StartNewCreatedVmInstanceReply r = (StartNewCreatedVmInstanceReply) reply;
                        completion.success(r.getVmInventory());
                    } else {
                        completion.fail(reply.getError());
                    }
                } catch (Exception e) {
                    bus.logExceptionWithMessageDump(msg, e);
                    bus.replyErrorByMessageType(msg, e);
                }
            }
        });
    }

    private void createVmButNotStart(CreateVmInstanceMsg msg, VmInstanceInventory inv) {
        StartVmFromNewCreatedStruct struct = StartVmFromNewCreatedStruct.fromMessage(msg);
        new JsonLabel().create(StartVmFromNewCreatedStruct.makeLabelKey(inv.getUuid()), struct, inv.getUuid());
    }

    private void handle(final CreateVmInstanceMsg msg) {
        doCreateVmInstance(msg, null, new ReturnValueCompletion<VmInstanceInventory>() {
            @Override
            public void success(VmInstanceInventory inv) {
                CreateVmInstanceReply reply = new CreateVmInstanceReply();
                reply.setInventory(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateVmInstanceReply r = new CreateVmInstanceReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    private CreateVmInstanceMsg fromAPICreateVmInstanceMsg(APICreateVmInstanceMsg msg) {
        CreateVmInstanceMsg cmsg = new CreateVmInstanceMsg();

        InstanceOfferingVO iovo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        cmsg.setInstanceOfferingUuid(iovo.getUuid());
        cmsg.setCpuNum(iovo.getCpuNum());
        cmsg.setCpuSpeed(iovo.getCpuSpeed());
        cmsg.setMemorySize(iovo.getMemorySize());
        cmsg.setAllocatorStrategy(iovo.getAllocatorStrategy());

        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
        cmsg.setName(msg.getName());
        cmsg.setImageUuid(msg.getImageUuid());
        cmsg.setL3NetworkUuids(msg.getL3NetworkUuids());
        cmsg.setType(msg.getType());
        cmsg.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        cmsg.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        cmsg.setZoneUuid(msg.getZoneUuid());
        cmsg.setClusterUuid(msg.getClusterUuid());
        cmsg.setHostUuid(msg.getHostUuid());
        cmsg.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        cmsg.setDescription(msg.getDescription());
        cmsg.setResourceUuid(msg.getResourceUuid());
        cmsg.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
        cmsg.setStrategy(msg.getStrategy());
        cmsg.setRootPassword(msg.getRootPassword());
        return cmsg;
    }

    private void handle(final APICreateVmInstanceMsg msg) {
        doCreateVmInstance(fromAPICreateVmInstanceMsg(msg), msg, new ReturnValueCompletion<VmInstanceInventory>() {
            APICreateVmInstanceEvent evt = new APICreateVmInstanceEvent(msg.getId());

            @Override
            public void success(VmInstanceInventory inv) {
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmInstanceConstant.SERVICE_ID);
    }

    private void createVmFlowChainBuilder() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        createVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(createVmWorkFlowElements).construct();
        stopVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(stopVmWorkFlowElements).construct();
        rebootVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(rebootVmWorkFlowElements).construct();
        startVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(startVmWorkFlowElements).construct();
        destroyVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(destroyVmWorkFlowElements).construct();
        migrateVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(migrateVmWorkFlowElements).construct();
        attachVolumeFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(attachVolumeWorkFlowElements).construct();
        attachIsoFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(attachIsoWorkFlowElements).construct();
        detachIsoFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(detachIsoWorkFlowElements).construct();
        expungeVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(expungeVmWorkFlowElements).construct();
        suspendVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(suspendVmWorkFlowElements).construct();
        resumeVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(resumeVmWorkFlowElements).construct();
    }

    private void populateExtensions() {
        for (VmInstanceFactory ext : pluginRgty.getExtensionList(VmInstanceFactory.class)) {
            VmInstanceFactory old = vmInstanceFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmInstanceFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            vmInstanceFactories.put(ext.getType().toString(), ext);
        }

        for (VmInstanceBaseExtensionFactory ext : pluginRgty.getExtensionList(VmInstanceBaseExtensionFactory.class)) {
            for (Class clz : ext.getMessageClasses()) {
                VmInstanceBaseExtensionFactory old = vmInstanceBaseExtensionFactories.get(clz);
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate VmInstanceBaseExtensionFactory[%s, %s] for the" +
                            " message[%s]", old.getClass(), ext.getClass(), clz));
                }

                vmInstanceBaseExtensionFactories.put(clz, ext);
            }
        }
    }

    @Override
    public boolean start() {
        try {
            createVmFlowChainBuilder();
            populateExtensions();
            installSystemTagValidator();
            installGlobalConfigUpdater();
            setupCanonicalEvents();

            bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
                @Override
                public void intercept(Message msg) {
                    if (msg instanceof NeedQuotaCheckMessage) {
                        if (((NeedQuotaCheckMessage) msg).getAccountUuid() == null ||
                                ((NeedQuotaCheckMessage) msg).getAccountUuid().equals("")) {
                            // skip admin scheduler
                            return;
                        }
                        List<Quota> quotas = acntMgr.getMessageQuotaMap().get(msg.getClass());
                        if (quotas == null || quotas.size() == 0) {
                            return;
                        }
                        Map<String, QuotaPair> pairs = new QuotaUtil().
                                makeQuotaPairs(((NeedQuotaCheckMessage) msg).getAccountUuid());
                        for (Quota quota : quotas) {
                            quota.getOperator().checkQuota((NeedQuotaCheckMessage) msg, pairs);
                        }
                    }
                }
            }, StartVmInstanceMsg.class);
            return true;
        } catch (Exception e) {
            throw new CloudConfigureFailException(VmInstanceManagerImpl.class, e.getMessage(), e);
        }
    }

    private void setupCanonicalEvents() {
        evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                HostStatusChangedData d = (HostStatusChangedData) data;
                if (!HostStatus.Disconnected.toString().equals(d.getNewStatus())) {
                    return;
                }

                if (!destMaker.isManagedByUs(d.getHostUuid())) {
                    return;
                }

                putVmToUnknownState(d.getHostUuid());
            }
        });
    }

    private void installGlobalConfigUpdater() {
        VmGlobalConfig.VM_EXPUNGE_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startVmExpungeTask();
            }
        });
        VmGlobalConfig.VM_EXPUNGE_PERIOD.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startVmExpungeTask();
            }
        });
        VmGlobalConfig.VM_DELETION_POLICY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startVmExpungeTask();
            }
        });
    }

    private void installSystemTagValidator() {
        class HostNameValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            private void validateHostname(String tag, String hostname) {
                DomainValidator domainValidator = DomainValidator.getInstance(true);
                if (!domainValidator.isValid(hostname)) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("hostname[%s] specified in system tag[%s] is not a valid domain name", hostname, tag)
                    ));
                }
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage cmsg) {
                final APICreateVmInstanceMsg msg = (APICreateVmInstanceMsg) cmsg;

                int hostnameCount = 0;
                for (String sysTag : msg.getSystemTags()) {
                    if (VmSystemTags.HOSTNAME.isMatch(sysTag)) {
                        if (++hostnameCount > 1) {
                            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                                    String.format("only one hostname system tag is allowed, but %s got", hostnameCount)
                            ));
                        }

                        String hostname = VmSystemTags.HOSTNAME.getTokenByTag(sysTag, VmSystemTags.HOSTNAME_TOKEN);

                        validateHostname(sysTag, hostname);
                        validateHostNameOnDefaultL3Network(sysTag, hostname, msg.getDefaultL3NetworkUuid());
                    } else if (VmSystemTags.STATIC_IP.isMatch(sysTag)) {
                        validateStaticIp(sysTag);
                    }
                }
            }

            private void validateStaticIp(String sysTag) {
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("L3 network[uuid:%s] not found. Please correct your system tag[%s] of static IP",
                                    l3Uuid, sysTag)
                    ));
                }

                String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
                if (!NetworkUtils.isIpv4Address(ip)) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("%s is not a valid IPv4 address. Please correct your system tag[%s] of static IP",
                                    ip, sysTag)
                    ));
                }

                CheckIpAvailabilityMsg cmsg = new CheckIpAvailabilityMsg();
                cmsg.setIp(ip);
                cmsg.setL3NetworkUuid(l3Uuid);
                bus.makeLocalServiceId(cmsg, L3NetworkConstant.SERVICE_ID);
                MessageReply r = bus.call(cmsg);
                if (!r.isSuccess()) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INTERNAL, r.getError()));
                }

                CheckIpAvailabilityReply cr = r.castReply();
                if (!cr.isAvailable()) {
                    throw new ApiMessageInterceptionException(errf.stringToOperationError(
                            String.format("IP[%s] is not available on the L3 network[uuid:%s]", ip, l3Uuid)
                    ));
                }
            }

            @Transactional(readOnly = true)
            private void validateHostNameOnDefaultL3Network(String tag, String hostname, String l3Uuid) {
                String sql = "select t" +
                        " from SystemTagVO t, VmInstanceVO vm, VmNicVO nic" +
                        " where t.resourceUuid = vm.uuid" +
                        " and vm.uuid = nic.vmInstanceUuid" +
                        " and nic.l3NetworkUuid = :l3Uuid" +
                        " and t.tag = :sysTag";
                TypedQuery<SystemTagVO> q = dbf.getEntityManager().createQuery(sql, SystemTagVO.class);
                q.setParameter("l3Uuid", l3Uuid);
                q.setParameter("sysTag", tag);
                List<SystemTagVO> vos = q.getResultList();

                if (!vos.isEmpty()) {
                    SystemTagVO sameTag = vos.get(0);
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("conflict hostname in system tag[%s];" +
                                            " there has been a VM[uuid:%s] having hostname[%s] on L3 network[uuid:%s]",
                                    tag, sameTag.getResourceUuid(), hostname, l3Uuid)
                    ));
                }
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (VmSystemTags.HOSTNAME.isMatch(systemTag)) {
                    String hostname = VmSystemTags.HOSTNAME.getTokenByTag(systemTag, VmSystemTags.HOSTNAME_TOKEN);
                    validateHostname(systemTag, hostname);

                    SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
                    q.select(VmInstanceVO_.defaultL3NetworkUuid);
                    q.add(VmInstanceVO_.uuid, Op.EQ, resourceUuid);
                    String defaultL3Uuid = q.findValue();

                    validateHostNameOnDefaultL3Network(systemTag, hostname, defaultL3Uuid);
                } else if (VmSystemTags.STATIC_IP.isMatch(systemTag)) {
                    validateStaticIp(systemTag);
                } else if (VmSystemTags.BOOT_ORDER.isMatch(systemTag)) {
                    validateBootOrder(systemTag);
                }
            }

            private void validateBootOrder(String systemTag) {
                String order = VmSystemTags.BOOT_ORDER.getTokenByTag(systemTag, VmSystemTags.BOOT_ORDER_TOKEN);
                for (String o : order.split(",")) {
                    try {
                        VmBootDevice.valueOf(o);
                    } catch (IllegalArgumentException e) {
                        throw new OperationFailureException(errf.stringToInvalidArgumentError(
                                String.format("invalid boot device[%s] in boot order[%s]", o, order)
                        ));
                    }
                }
            }
        }

        HostNameValidator hostnameValidator = new HostNameValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), hostnameValidator);
        VmSystemTags.HOSTNAME.installValidator(hostnameValidator);
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public VmInstanceFactory getVmInstanceFactory(VmInstanceType type) {
        VmInstanceFactory factory = vmInstanceFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No VmInstanceFactory of type[%s] found", type));
        }
        return factory;
    }

    @Override
    public VmInstanceBaseExtensionFactory getVmInstanceBaseExtensionFactory(Message msg) {
        return vmInstanceBaseExtensionFactories.get(msg.getClass());
    }

    @Transactional
    protected void putVmToUnknownState(final String hostUuid) {
        SimpleQuery<VmInstanceVO> query = dbf.createQuery(VmInstanceVO.class);
        query.select(VmInstanceVO_.uuid);
        query.add(VmInstanceVO_.hostUuid, Op.EQ, hostUuid);
        List<String> tss = query.listValue();
        List<VmStateChangedOnHostMsg> msgs = CollectionUtils.transformToList(tss, new Function<VmStateChangedOnHostMsg, String>() {
            @Override
            public VmStateChangedOnHostMsg call(String vmUuid) {
                VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
                msg.setVmInstanceUuid(vmUuid);
                msg.setHostUuid(hostUuid);
                msg.setStateOnHost(VmInstanceState.Unknown);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                return msg;
            }
        });
        bus.send(msgs);
    }

    @Override
    public FlowChain getCreateVmWorkFlowChain(VmInstanceInventory inv) {
        return createVmFlowBuilder.build();
    }

    @Override
    public FlowChain getStopVmWorkFlowChain(VmInstanceInventory inv) {
        return stopVmFlowBuilder.build();
    }

    @Override
    public FlowChain getRebootVmWorkFlowChain(VmInstanceInventory inv) {
        return rebootVmFlowBuilder.build();
    }

    @Override
    public FlowChain getStartVmWorkFlowChain(VmInstanceInventory inv) {
        return startVmFlowBuilder.build();
    }

    @Override
    public FlowChain getDestroyVmWorkFlowChain(VmInstanceInventory inv) {
        return destroyVmFlowBuilder.build();
    }

    @Override
    public FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv) {
        return migrateVmFlowBuilder.build();
    }

    @Override
    public FlowChain getAttachUninstantiatedVolumeWorkFlowChain(VmInstanceInventory inv) {
        return attachVolumeFlowBuilder.build();
    }


    @Override
    public FlowChain getAttachIsoWorkFlowChain(VmInstanceInventory inv) {
        return attachIsoFlowBuilder.build();
    }

    @Override
    public FlowChain getDetachIsoWorkFlowChain(VmInstanceInventory inv) {
        return detachIsoFlowBuilder.build();
    }

    @Override
    public FlowChain getExpungeVmWorkFlowChain(VmInstanceInventory inv) {
        return expungeVmFlowBuilder.build();
    }

    public FlowChain getSuspendWorkFlowChain(VmInstanceInventory inv){
        return suspendVmFlowBuilder.build();
    }

    public FlowChain getResumeVmWorkFlowChain(VmInstanceInventory inv){
        return resumeVmFlowBuilder.build();
    }

    public void setCreateVmWorkFlowElements(List<String> createVmWorkFlowElements) {
        this.createVmWorkFlowElements = createVmWorkFlowElements;
    }

    public void setStopVmWorkFlowElements(List<String> stopVmWorkFlowElements) {
        this.stopVmWorkFlowElements = stopVmWorkFlowElements;
    }

    public void setRebootVmWorkFlowElements(List<String> rebootVmWorkFlowElements) {
        this.rebootVmWorkFlowElements = rebootVmWorkFlowElements;
    }

    public void setStartVmWorkFlowElements(List<String> startVmWorkFlowElements) {
        this.startVmWorkFlowElements = startVmWorkFlowElements;
    }

    public void setDestroyVmWorkFlowElements(List<String> destroyVmWorkFlowElements) {
        this.destroyVmWorkFlowElements = destroyVmWorkFlowElements;
    }

    public void setMigrateVmWorkFlowElements(List<String> migrateVmWorkFlowElements) {
        this.migrateVmWorkFlowElements = migrateVmWorkFlowElements;
    }

    public void setAttachVolumeWorkFlowElements(List<String> attachVolumeWorkFlowElements) {
        this.attachVolumeWorkFlowElements = attachVolumeWorkFlowElements;
    }

    public void setAttachIsoWorkFlowElements(List<String> attachIsoWorkFlowElements) {
        this.attachIsoWorkFlowElements = attachIsoWorkFlowElements;
    }

    public void setDetachIsoWorkFlowElements(List<String> detachIsoWorkFlowElements) {
        this.detachIsoWorkFlowElements = detachIsoWorkFlowElements;
    }

    public void setExpungeVmWorkFlowElements(List<String> expungeVmWorkFlowElements) {
        this.expungeVmWorkFlowElements = expungeVmWorkFlowElements;
    }

    public void setSuspendVmWorkFlowElements(List<String> suspendVmWorkFlowElements){
        this.suspendVmWorkFlowElements = suspendVmWorkFlowElements;
    }

    public void setResumeVmWorkFlowElements(List<String> resumeVmWorkFlowElements){
        this.resumeVmWorkFlowElements = resumeVmWorkFlowElements;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                AccountType type = new QuotaUtil().getAccountType(msg.getSession().getAccountUuid());

                if (type != AccountType.SystemAdmin) {
                    if (msg instanceof APICreateVmInstanceMsg) {
                        if (((APICreateVmInstanceMsg) msg).getStrategy().
                                equals(VmCreationStrategy.JustCreate.toString())) {
                            return;
                        }
                        check((APICreateVmInstanceMsg) msg, pairs);
                    } else if (msg instanceof APICreateDataVolumeMsg) {
                        check((APICreateDataVolumeMsg) msg, pairs);
                    } else if (msg instanceof APIRecoverDataVolumeMsg) {
                        check((APIRecoverDataVolumeMsg) msg, pairs);
                    } else if (msg instanceof APIStartVmInstanceMsg) {
                        check((APIStartVmInstanceMsg) msg, pairs);
                    } else if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    } else if (msg instanceof APIRecoverVmInstanceMsg) {
                        check((APIRecoverVmInstanceMsg) msg, pairs);
                    } else if (msg instanceof APICreateSchedulerMessage) {
                        check((APICreateSchedulerMessage) msg, pairs);
                    }
                } else {
                    if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getAccountUuid())) {
                    if (msg instanceof StartVmInstanceMsg) {
                        check((StartVmInstanceMsg) msg, pairs);
                    }
                }
            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                List<Quota.QuotaUsage> usages = new ArrayList<>();

                VmQuotaUtil.VmQuota vmQuota = new VmQuotaUtil().getUsedVmCpuMemory(accountUuid);
                Quota.QuotaUsage usage;

                usage = new Quota.QuotaUsage();
                usage.setName(VmInstanceConstant.QUOTA_VM_TOTAL_NUM);
                usage.setUsed(vmQuota.totalVmNum);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VmInstanceConstant.QUOTA_VM_RUNNING_NUM);
                usage.setUsed(vmQuota.runningVmNum);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM);
                usage.setUsed(vmQuota.runningVmCpuNum);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE);
                usage.setUsed(vmQuota.runningVmMemorySize);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VolumeConstant.QUOTA_DATA_VOLUME_NUM);
                usage.setUsed(new VmQuotaUtil().getUsedDataVolumeCount(accountUuid));
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VolumeConstant.QUOTA_VOLUME_SIZE);
                usage.setUsed(new VmQuotaUtil().getUsedAllVolumeSize(accountUuid));
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(SchedulerConstant.QUOTA_SCHEDULER_NUM);
                usage.setUsed(new VmQuotaUtil().getUsedSchedulerNum(accountUuid));
                usages.add(usage);

                return usages;
            }


            private void check(APIStartVmInstanceMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkStartVmInstance(msg.getSession().getAccountUuid(), msg.getVmInstanceUuid(), pairs);
            }

            private void check(StartVmInstanceMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkStartVmInstance(msg.getAccountUuid(), msg.getVmInstanceUuid(), pairs);
            }

            private void checkStartVmInstance(String currentAccountUuid,
                                              String vmInstanceUuid,
                                              Map<String, Quota.QuotaPair> pairs) {
                String resourceTargetOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(vmInstanceUuid);
                checkVmInstanceQuota(currentAccountUuid, resourceTargetOwnerAccountUuid, vmInstanceUuid, pairs);
            }

            @Transactional(readOnly = true)
            private void checkVmInstanceQuota(String currentAccountUuid,
                                              String resourceTargetOwnerAccountUuid,
                                              String vmInstanceUuid,
                                              Map<String, Quota.QuotaPair> pairs) {
                long vmNumQuota = pairs.get(VmInstanceConstant.QUOTA_VM_RUNNING_NUM).getValue();
                long cpuNumQuota = pairs.get(VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM).getValue();
                long memoryQuota = pairs.get(VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE).getValue();

                VmQuotaUtil.VmQuota vmQuotaUsed = new VmQuotaUtil().getUsedVmCpuMemory(resourceTargetOwnerAccountUuid);
                //
                {
                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VmInstanceConstant.QUOTA_VM_RUNNING_NUM;
                    quotaCompareInfo.quotaValue = vmNumQuota;
                    quotaCompareInfo.currentUsed = vmQuotaUsed.runningVmNum;
                    quotaCompareInfo.request = 1;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
                //
                VmInstanceVO vm = dbf.getEntityManager().find(VmInstanceVO.class, vmInstanceUuid);
                {
                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM;
                    quotaCompareInfo.quotaValue = cpuNumQuota;
                    quotaCompareInfo.currentUsed = vmQuotaUsed.runningVmCpuNum;
                    quotaCompareInfo.request = vm.getCpuNum();
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
                {
                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE;
                    quotaCompareInfo.quotaValue = memoryQuota;
                    quotaCompareInfo.currentUsed = vmQuotaUsed.runningVmMemorySize;
                    quotaCompareInfo.request = vm.getMemorySize();
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }


            private void checkVolumeQuotaForChangeResourceOwner(List<String> dataVolumeUuids,
                                                                List<String> rootVolumeUuids,
                                                                String resourceTargetOwnerAccountUuid,
                                                                String currentAccountUuid,
                                                                Map<String, Quota.QuotaPair> pairs) {
                long dataVolumeNumQuota = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long allVolumeSizeQuota = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();

                ArrayList<String> volumeUuids = new ArrayList<>();
                if (dataVolumeUuids != null && !dataVolumeUuids.isEmpty()) {
                    for (String uuid : dataVolumeUuids) {
                        volumeUuids.add(uuid);
                    }
                }
                if (rootVolumeUuids != null && !rootVolumeUuids.isEmpty()) {
                    for (String uuid : rootVolumeUuids) {
                        volumeUuids.add(uuid);
                    }
                }

                // skip empty volume uuid list
                if (volumeUuids.isEmpty()) {
                    return;
                }
                // check data volume num
                long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(resourceTargetOwnerAccountUuid);
                if (dataVolumeUuids != null && !dataVolumeUuids.isEmpty()) {
                    long dataVolumeNumAsked = dataVolumeUuids.size();
                    {
                        QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                        quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                        quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                        quotaCompareInfo.quotaName = VolumeConstant.QUOTA_DATA_VOLUME_NUM;
                        quotaCompareInfo.quotaValue = dataVolumeNumQuota;
                        quotaCompareInfo.currentUsed = dataVolumeNumUsed;
                        quotaCompareInfo.request = dataVolumeNumAsked;
                        new QuotaUtil().CheckQuota(quotaCompareInfo);
                    }
                }

                // check data volume size
                long allVolumeSizeAsked;
                String sql = "select sum(size) from VolumeVO where uuid in (:uuids) ";
                TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                dq.setParameter("uuids", volumeUuids);
                Long dsize = dq.getSingleResult();
                dsize = dsize == null ? 0 : dsize;
                allVolumeSizeAsked = dsize;

                long allVolumeSizeUsed = new VmQuotaUtil().getUsedAllVolumeSize(resourceTargetOwnerAccountUuid);
                {
                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VolumeConstant.QUOTA_VOLUME_SIZE;
                    quotaCompareInfo.quotaValue = allVolumeSizeQuota;
                    quotaCompareInfo.currentUsed = allVolumeSizeUsed;
                    quotaCompareInfo.request = allVolumeSizeAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }


            private void checkRunningVMQuotaForChangeResourceOwner(String vmInstanceUuid,
                                                                   String resourceTargetOwnerAccountUuid,
                                                                   String currentAccountUuid,
                                                                   Map<String, Quota.QuotaPair> pairs) {
                checkVmInstanceQuota(currentAccountUuid, resourceTargetOwnerAccountUuid, vmInstanceUuid, pairs);
            }

            @Transactional(readOnly = true)
            private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
                if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
                    return;
                }

                String resourceType = new QuotaUtil().getResourceType(msg.getResourceUuid());
                if (resourceType.equals(VolumeVO.class.getSimpleName())) {
                    String volumeUuid = msg.getResourceUuid();
                    ArrayList<String> volumeUuids = new ArrayList<>();
                    volumeUuids.add(volumeUuid);
                    checkVolumeQuotaForChangeResourceOwner(volumeUuids, null,
                            resourceTargetOwnerAccountUuid, currentAccountUuid, pairs);

                } else if (resourceType.equals(VmInstanceVO.class.getSimpleName())) {
                    VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getResourceUuid(), VmInstanceVO.class);

                    // filter vm state
                    if (vmInstanceVO.getState().equals(VmInstanceState.Created)) {
                        return;
                    } else if (!vmInstanceVO.getState().equals(VmInstanceState.Stopped)
                            && !vmInstanceVO.getState().equals(VmInstanceState.Running)) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(VmErrors.NOT_IN_CORRECT_STATE,
                                String.format("Incorrect VM State.VM[uuid:%s] current state:%s. ",
                                        msg.getResourceUuid(), vmInstanceVO.getState())
                        ));
                    }

                    String vmInstanceUuid = msg.getResourceUuid();

                    // check vm
                    if (vmInstanceVO.getState().equals(VmInstanceState.Running)) {
                        checkRunningVMQuotaForChangeResourceOwner(vmInstanceUuid, resourceTargetOwnerAccountUuid,
                                currentAccountUuid, pairs);
                    }

                    // check volume
                    ArrayList<String> rootVolumeUuids = new ArrayList<>();
                    SimpleQuery<VolumeVO> sq = dbf.createQuery(VolumeVO.class);
                    sq.add(VolumeVO_.vmInstanceUuid, Op.EQ, vmInstanceUuid);
                    sq.add(VolumeVO_.type, Op.EQ, VolumeType.Root);
                    VolumeVO volumeVO = sq.find();
                    if (volumeVO != null) {
                        rootVolumeUuids.add(volumeVO.getUuid());
                    }

                    ArrayList<String> dataVolumeUuids = new ArrayList<>();
                    SimpleQuery<VolumeVO> sq1 = dbf.createQuery(VolumeVO.class);
                    sq1.add(VolumeVO_.vmInstanceUuid, Op.EQ, vmInstanceUuid);
                    sq1.add(VolumeVO_.type, Op.EQ, VolumeType.Data);
                    List<VolumeVO> volumeVOs = sq1.list();
                    if (volumeVOs != null && !volumeVOs.isEmpty()) {
                        for (VolumeVO vvo : volumeVOs) {
                            dataVolumeUuids.add(vvo.getUuid());
                        }
                    }

                    checkVolumeQuotaForChangeResourceOwner(dataVolumeUuids, rootVolumeUuids,
                            resourceTargetOwnerAccountUuid, currentAccountUuid, pairs);
                }

            }

            private void check(APIRecoverDataVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getVolumeUuid());
                // check data volume num
                long dataVolumeNumQuota = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(resourceTargetOwnerAccountUuid);
                long dataVolumeNumAsked = 1;

                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VolumeConstant.QUOTA_DATA_VOLUME_NUM;
                    quotaCompareInfo.quotaValue = dataVolumeNumQuota;
                    quotaCompareInfo.currentUsed = dataVolumeNumUsed;
                    quotaCompareInfo.request = dataVolumeNumAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }

            @Transactional(readOnly = true)
            private void check(APICreateDataVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                long dataVolumeNumQuota = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long allVolumeSizeQuota = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();

                // check data volume num
                long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(currentAccountUuid);
                long dataVolumeNumAsked = 1;
                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VolumeConstant.QUOTA_DATA_VOLUME_NUM;
                    quotaCompareInfo.quotaValue = dataVolumeNumQuota;
                    quotaCompareInfo.currentUsed = dataVolumeNumUsed;
                    quotaCompareInfo.request = dataVolumeNumAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }

                // check data volume size
                long allVolumeSizeAsked;
                String sql = "select diskSize from DiskOfferingVO where uuid = :uuid ";
                TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                dq.setParameter("uuid", msg.getDiskOfferingUuid());
                Long dsize = dq.getSingleResult();
                dsize = dsize == null ? 0 : dsize;
                allVolumeSizeAsked = dsize;

                long allVolumeSizeUsed = new VmQuotaUtil().getUsedAllVolumeSize(currentAccountUuid);
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VolumeConstant.QUOTA_VOLUME_SIZE;
                    quotaCompareInfo.quotaValue = allVolumeSizeQuota;
                    quotaCompareInfo.currentUsed = allVolumeSizeUsed;
                    quotaCompareInfo.request = allVolumeSizeAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }

            @Transactional(readOnly = true)
            private void check(APICreateVmInstanceMsg msg, Map<String, QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                long totalVmNumQuota = pairs.get(VmInstanceConstant.QUOTA_VM_TOTAL_NUM).getValue();
                long runningVmNumQuota = pairs.get(VmInstanceConstant.QUOTA_VM_RUNNING_NUM).getValue();
                long runningVmCpuNumQuota = pairs.get(VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM).getValue();
                long runningVmMemorySizeQuota = pairs.get(VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE).getValue();
                long dataVolumeNumQuota = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long allVolumeSizeQuota = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();


                VmQuotaUtil.VmQuota vmQuotaUsed = new VmQuotaUtil().getUsedVmCpuMemory(currentAccountUuid);

                if (vmQuotaUsed.totalVmNum + 1 > totalVmNumQuota) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, VmInstanceConstant.QUOTA_VM_TOTAL_NUM, totalVmNumQuota)
                    ));
                }

                if (vmQuotaUsed.runningVmNum + 1 > runningVmNumQuota) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, VmInstanceConstant.QUOTA_VM_RUNNING_NUM, runningVmNumQuota)
                    ));
                }

                String sql = "select i.cpuNum, i.memorySize" +
                        " from InstanceOfferingVO i" +
                        " where i.uuid = :uuid";
                TypedQuery<Tuple> iq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                iq.setParameter("uuid", msg.getInstanceOfferingUuid());
                Tuple it = iq.getSingleResult();
                int cpuNumAsked = it.get(0, Integer.class);
                long memoryAsked = it.get(1, Long.class);

                if (vmQuotaUsed.runningVmCpuNum + cpuNumAsked > runningVmCpuNumQuota) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM, runningVmCpuNumQuota)
                    ));
                }

                if (vmQuotaUsed.runningVmMemorySize + memoryAsked > runningVmMemorySizeQuota) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE, runningVmMemorySizeQuota)
                    ));
                }

                // check data volume num
                if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
                    long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(currentAccountUuid);
                    long dataVolumeNumAsked = msg.getDataDiskOfferingUuids().size();
                    if (dataVolumeNumUsed + dataVolumeNumAsked > dataVolumeNumQuota) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        currentAccountUuid, VolumeConstant.QUOTA_DATA_VOLUME_NUM, dataVolumeNumQuota)
                        ));
                    }
                }

                // check all volume size
                long allVolumeSizeAsked = 0;

                sql = "select img.size, img.mediaType" +
                        " from ImageVO img" +
                        " where img.uuid = :iuuid";
                iq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                iq.setParameter("iuuid", msg.getImageUuid());
                it = iq.getSingleResult();
                Long imgSize = it.get(0, Long.class);
                ImageMediaType imgType = it.get(1, ImageMediaType.class);

                List<String> diskOfferingUuids = new ArrayList<>();
                if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
                    diskOfferingUuids.addAll(msg.getDataDiskOfferingUuids());
                }
                if (imgType == ImageMediaType.RootVolumeTemplate) {
                    allVolumeSizeAsked += imgSize;
                } else if (imgType == ImageMediaType.ISO) {
                    diskOfferingUuids.add(msg.getRootDiskOfferingUuid());
                }

                HashMap<String, Long> diskOfferingCountMap = new HashMap<>();
                if (!diskOfferingUuids.isEmpty()) {
                    for (String diskOfferingUuid : diskOfferingUuids) {
                        if (diskOfferingCountMap.containsKey(diskOfferingUuid)) {
                            diskOfferingCountMap.put(diskOfferingUuid, diskOfferingCountMap.get(diskOfferingUuid) + 1);
                        } else {
                            diskOfferingCountMap.put(diskOfferingUuid, 1L);
                        }
                    }
                    for (String diskOfferingUuid : diskOfferingCountMap.keySet()) {
                        sql = "select diskSize from DiskOfferingVO where uuid = :uuid";
                        TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                        dq.setParameter("uuid", diskOfferingUuid);
                        Long dsize = dq.getSingleResult();
                        dsize = dsize == null ? 0 : dsize;
                        allVolumeSizeAsked += dsize * diskOfferingCountMap.get(diskOfferingUuid);
                    }
                }

                long allVolumeSizeUsed = new VmQuotaUtil().getUsedAllVolumeSize(currentAccountUuid);
                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VolumeConstant.QUOTA_VOLUME_SIZE;
                    quotaCompareInfo.quotaValue = allVolumeSizeQuota;
                    quotaCompareInfo.currentUsed = allVolumeSizeUsed;
                    quotaCompareInfo.request = allVolumeSizeAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }

            private void check(APIRecoverVmInstanceMsg msg, Map<String, QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                long totalVmNumQuota = pairs.get(VmInstanceConstant.QUOTA_VM_TOTAL_NUM).getValue();
                VmQuotaUtil.VmQuota vmQuotaUsed = new VmQuotaUtil().getUsedVmCpuMemory(currentAccountUuid);
                long totalVmNumAsked = 1;
                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                {
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VmInstanceConstant.QUOTA_VM_TOTAL_NUM;
                    quotaCompareInfo.quotaValue = totalVmNumQuota;
                    quotaCompareInfo.currentUsed = vmQuotaUsed.totalVmNum;
                    quotaCompareInfo.request = totalVmNumAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }

            private void check(APICreateSchedulerMessage msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                long schedulerNumQuota = pairs.get(SchedulerConstant.QUOTA_SCHEDULER_NUM).getValue();
                long schedulerNumUsed = new VmQuotaUtil().getUsedSchedulerNum(resourceTargetOwnerAccountUuid);
                {
                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = SchedulerConstant.QUOTA_SCHEDULER_NUM;
                    quotaCompareInfo.quotaValue = schedulerNumQuota;
                    quotaCompareInfo.currentUsed = schedulerNumUsed;
                    quotaCompareInfo.request = 1;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }
        };

        Quota quota = new Quota();
        QuotaPair p;

        p = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_TOTAL_NUM);
        p.setValue(20);
        quota.addPair(p);

        p = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_RUNNING_NUM);
        p.setValue(20);
        quota.addPair(p);

        p = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM);
        p.setValue(80);
        quota.addPair(p);

        p = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE);
        p.setValue(SizeUnit.GIGABYTE.toByte(80));
        quota.addPair(p);

        p = new QuotaPair();
        p.setName(VolumeConstant.QUOTA_DATA_VOLUME_NUM);
        p.setValue(40);
        quota.addPair(p);

        p = new QuotaPair();
        p.setName(VolumeConstant.QUOTA_VOLUME_SIZE);
        p.setValue(SizeUnit.TERABYTE.toByte(10));
        quota.addPair(p);

        p = new Quota.QuotaPair();
        p.setName(SchedulerConstant.QUOTA_SCHEDULER_NUM);
        p.setValue(80);
        quota.addPair(p);

        quota.addMessageNeedValidation(APICreateVmInstanceMsg.class);
        quota.addMessageNeedValidation(APIRecoverVmInstanceMsg.class);
        quota.addMessageNeedValidation(APICreateDataVolumeMsg.class);
        quota.addMessageNeedValidation(APIRecoverDataVolumeMsg.class);
        quota.addMessageNeedValidation(APIStartVmInstanceMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
        quota.addMessageNeedValidation(StartVmInstanceMsg.class);
        // scheduler
        quota.addMessageNeedValidation(APICreateSchedulerMessage.class);
        quota.addMessageNeedValidation(APICreateStartVmInstanceSchedulerMsg.class);
        quota.addMessageNeedValidation(APICreateVolumeSnapshotSchedulerMsg.class);
        quota.addMessageNeedValidation(APICreateRebootVmInstanceSchedulerMsg.class);
        quota.addMessageNeedValidation(APICreateStopVmInstanceSchedulerMsg.class);


        quota.setOperator(checker);

        return list(quota);

    }

    private List<String> getVmInUnknownStateManagedByUs() {
        int qun = 10000;
        SimpleQuery q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Unknown);
        long amount = q.count();
        int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
        int start = 0;
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid);
            q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Unknown);
            q.setLimit(qun);
            q.setStart(start);
            List<Tuple> lst = q.listTuple();
            start += qun;
            for (Tuple t : lst) {
                String vmUuid = t.get(0, String.class);
                if (!destMaker.isManagedByUs(vmUuid)) {
                    continue;
                }

                String hostUuid = t.get(1, String.class);
                if (hostUuid == null) {
                    //TODO
                    logger.warn(String.format("the vm[uuid:%s] is in Unknown state, but its hostUuid is null," +
                            " we cannot check its real state", vmUuid));
                    continue;
                }

                ret.add(vmUuid);
            }
        }
        return ret;
    }

    private void checkUnknownVm() {
        List<String> unknownVmUuids = getVmInUnknownStateManagedByUs();
        if (unknownVmUuids.isEmpty()) {
            return;
        }

        for (final String uuid : unknownVmUuids) {
            TimeBasedGCEphemeralContext<Void> context = new TimeBasedGCEphemeralContext<>();
            context.setInterval(5);
            context.setTimeUnit(TimeUnit.SECONDS);
            context.setRunner(new GCRunner() {
                @Override
                public void run(GCContext context, final GCCompletion completion) {
                    VmCheckOwnStateMsg msg = new VmCheckOwnStateMsg();
                    msg.setVmInstanceUuid(uuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, uuid);
                    bus.send(msg, new CloudBusCallBack() {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                completion.success();
                            } else {
                                completion.fail(errf.stringToOperationError(
                                        String.format("unable to check the vm[uuid:%s]'s state, %s", uuid, reply.getError())
                                ));
                            }
                        }
                    });
                }
            });
            gcf.scheduleImmediately(context);
        }
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        //checkUnknownVm();
        startVmExpungeTask();
    }

    private synchronized void startVmExpungeTask() {
        if (expungeVmTask != null) {
            expungeVmTask.cancel(true);
        }

        expungeVmTask = thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {

            private List<Tuple> getVmDeletedStateManagedByUs() {
                int qun = 10000;
                SimpleQuery q = dbf.createQuery(VmInstanceVO.class);
                q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Destroyed);
                long amount = q.count();
                int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
                int start = 0;
                List<Tuple> ret = new ArrayList<>();
                for (int i = 0; i < times; i++) {
                    q = dbf.createQuery(VmInstanceVO.class);
                    q.select(VmInstanceVO_.uuid, VmInstanceVO_.lastOpDate);
                    q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Destroyed);
                    q.setLimit(qun);
                    q.setStart(start);
                    List<Tuple> ts = q.listTuple();
                    start += qun;

                    for (Tuple t : ts) {
                        String vmUuid = t.get(0, String.class);
                        if (!destMaker.isManagedByUs(vmUuid)) {
                            continue;
                        }
                        ret.add(t);
                    }
                }

                return ret;
            }

            @Override
            public synchronized boolean run() {
                final List<Tuple> vms = getVmDeletedStateManagedByUs();
                if (vms.isEmpty()) {
                    logger.debug("[VM Expunging Task]: no vm to expunge");
                    return false;
                }

                final Timestamp current = dbf.getCurrentSqlTime();

                final List<ExpungeVmMsg> msgs = CollectionUtils.transformToList(vms, new Function<ExpungeVmMsg, Tuple>() {
                    @Override
                    public ExpungeVmMsg call(Tuple t) {
                        String uuid = t.get(0, String.class);
                        Timestamp date = t.get(1, Timestamp.class);
                        long end = date.getTime() + TimeUnit.SECONDS.toMillis(VmGlobalConfig.VM_EXPUNGE_PERIOD.value(Long.class));
                        if (current.getTime() >= end) {
                            VmInstanceDeletionPolicy deletionPolicy = deletionPolicyMgr.getDeletionPolicy(uuid);

                            if (deletionPolicy == VmInstanceDeletionPolicy.Never) {
                                logger.debug(String.format("[VM Expunging Task]: the deletion policy of the vm[uuid:%s] is Never, don't expunge it",
                                        uuid));
                                return null;
                            } else {
                                ExpungeVmMsg msg = new ExpungeVmMsg();
                                msg.setVmInstanceUuid(uuid);
                                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, uuid);
                                return msg;
                            }
                        } else {
                            return null;
                        }
                    }
                });

                if (msgs.isEmpty()) {
                    logger.debug("[VM Expunging Task]: no vm to expunge");
                    return false;
                }

                bus.send(msgs, 100, new CloudBusListCallBack() {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply r : replies) {
                            ExpungeVmMsg msg = msgs.get(replies.indexOf(r));
                            if (!r.isSuccess()) {
                                logger.warn(String.format("failed to expunge the vm[uuid:%s], %s",
                                        msg.getVmInstanceUuid(), r.getError()));
                            } else {
                                logger.debug(String.format("successfully expunged the vm[uuid:%s]",
                                        msg.getVmInstanceUuid()));
                            }
                        }
                    }
                });

                return false;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return VmGlobalConfig.VM_EXPUNGE_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "expunge-vm-task";
            }
        });

        logger.debug(String.format("vm expunging task starts running, [period: %s seconds, interval: %s seconds]",
                VmGlobalConfig.VM_EXPUNGE_PERIOD.value(Long.class), VmGlobalConfig.VM_EXPUNGE_INTERVAL.value(Long.class)));
    }

    @Override
    public String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException {
        return null;
    }

    @Override
    public void beforeDeleteL3Network(L3NetworkInventory inventory) {
    }

    @Override
    public void afterDeleteL3Network(L3NetworkInventory inventory) {
        new StaticIpOperator().deleteStaticIpByL3NetworkUuid(inventory.getUuid());
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        // change root volume
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.rootVolumeUuid);
        q.add(VmInstanceVO_.uuid, Op.EQ, ref.getResourceUuid());
        String rootVolumeUuid = q.findValue();
        if (rootVolumeUuid == null) {
            return;
        }

        acntMgr.changeResourceOwner(rootVolumeUuid, newOwnerUuid);

        // change vmnic(s)
        SimpleQuery<VmNicVO> sq = dbf.createQuery(VmNicVO.class);
        sq.select(VmNicVO_.uuid);
        sq.add(VmNicVO_.vmInstanceUuid, Op.EQ, ref.getResourceUuid());
        List<String> vmnics = sq.listValue();
        if (vmnics.isEmpty()) {
            return;
        }
        for (String vmnicUuid : vmnics) {
            acntMgr.changeResourceOwner(vmnicUuid, newOwnerUuid);
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIChangeResourceOwnerMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIChangeResourceOwnerMsg) {
            validateAPIChangeResourceOwnerMsg((APIChangeResourceOwnerMsg) msg);
        }

        return msg;
    }

    private void validateAPIChangeResourceOwnerMsg(APIChangeResourceOwnerMsg msg) {
        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, msg.getResourceUuid());
        AccountResourceRefVO ref = q.find();

        if (ref == null || !VolumeVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        SimpleQuery<VolumeVO> vq = dbf.createQuery(VolumeVO.class);
        vq.add(VolumeVO_.uuid, Op.EQ, ref.getResourceUuid());
        vq.add(VolumeVO_.type, Op.EQ, VolumeType.Root);
        if (vq.isExists()) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("the resource[uuid:%s] is a ROOT volume, you cannot change its owner, instead," +
                            "change the owner of the VM the root volume belongs to", ref.getResourceUuid())
            ));
        }
    }

    public void preDeletePrimaryStorage(PrimaryStorageInventory inv) throws PrimaryStorageException {

    }

    public void beforeDeletePrimaryStorage(PrimaryStorageInventory inv) {

    }

    public void afterDeletePrimaryStorage(PrimaryStorageInventory inv) {
        String sql = "select vm.name,vm.uuid" +
                " from VmInstanceVO vm, VolumeVO vol" +
                " where vm.uuid = vol.vmInstanceUuid" +
                " and vm.state = 'Destroyed'" +
                " and vol.type = 'Root'" +
                " and vol.primaryStorageUuid = :primaryUuid";

        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("primaryUuid", inv.getUuid());
        List<Tuple> vms = q.getResultList();
        if (vms.isEmpty()) {
            return;
        }

        for (Tuple vm : vms) {
            APIExpungeVmInstanceMsg msg = new APIExpungeVmInstanceMsg();
            msg.setUuid(vm.get(1, String.class));
            passThrough(msg);
        }

    }
}
