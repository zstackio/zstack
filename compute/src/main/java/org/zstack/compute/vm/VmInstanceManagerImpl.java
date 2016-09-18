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
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.core.scheduler.SchedulerVO_;
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
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.search.SearchOp;
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
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionDSL.map;

public class VmInstanceManagerImpl extends AbstractService implements VmInstanceManager,
        ReportQuotaExtensionPoint, ManagementNodeReadyExtensionPoint, L3NetworkDeleteExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint, GlobalApiMessageInterceptor, VmInstanceDestroyExtensionPoint,
        RecoverVmExtensionPoint, VmBeforeExpungeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmInstanceManagerImpl.class);
    private Map<String, VmInstanceFactory> vmInstanceFactories = Collections.synchronizedMap(new HashMap<String, VmInstanceFactory>());
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
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();
    private Future<Void> expungeVmTask;

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
        } else if (msg instanceof VmInstanceMessage) {
            passThrough((VmInstanceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetInterdependentL3NetworksImagesMsg msg) {
        if (msg.getImageUuid() != null) {
            getInterdependentL3NetworksByImageUuid(msg);
        } else {
            getInterdependentImagesByL3NetworkUuids(msg);
        }
    }

    private List<String> listIntersection(List<String> a, List<String> b) {
        List<String> ret = new ArrayList<>();
        for (String s : a) {
            if (b.contains(s)) {
                ret.add(s);
            }
        }

        return ret;
    }

    @Transactional(readOnly = true)
    private void getInterdependentImagesByL3NetworkUuids(APIGetInterdependentL3NetworksImagesMsg msg) {
        APIGetInterdependentL3NetworkImageReply reply = new APIGetInterdependentL3NetworkImageReply();

        List<List<String>> listBsTypes = new ArrayList<>();
        List<String> allBsTypes = new ArrayList<>();
        for (String l3uuid : msg.getL3NetworkUuids()) {
            String sql = "select ps.type from PrimaryStorageVO ps, L2NetworkClusterRefVO l2ref, L3NetworkVO l3, PrimaryStorageClusterRefVO psref" +
                    " where ps.uuid = psref.primaryStorageUuid and psref.clusterUuid = l2ref.clusterUuid" +
                    " and l2ref.l2NetworkUuid = l3.l2NetworkUuid and l3.uuid = :l3uuid";
            TypedQuery<String> psq = dbf.getEntityManager().createQuery(sql, String.class);
            psq.setParameter("l3uuid", l3uuid);
            List<String> l = psq.getResultList();

            List<String> bsTypes = new ArrayList<>();
            for (String psType : l) {
                bsTypes.addAll(hostAllocatorMgr.getBackupStorageTypesByPrimaryStorageTypeFromMetrics(psType));
            }
            listBsTypes.add(bsTypes);
            allBsTypes.addAll(bsTypes);
        }

        List<String> bsTypes = allBsTypes;
        for (List<String> bss : listBsTypes) {
            bsTypes = listIntersection(bsTypes, bss);
        }

        if (bsTypes.isEmpty()) {
            reply.setInventories(new ArrayList());
            bus.reply(msg, reply);
            return;
        }

        String sql = "select img from ImageVO img, ImageBackupStorageRefVO iref, BackupStorageZoneRefVO zref, BackupStorageVO bs where" +
                " img.uuid = iref.imageUuid and iref.backupStorageUuid = zref.backupStorageUuid" +
                " and bs.uuid = zref.backupStorageUuid and bs.type in (:bsTypes) and zref.zoneUuid = :zoneUuid group by img.uuid";
        TypedQuery<ImageVO> iq = dbf.getEntityManager().createQuery(sql, ImageVO.class);
        iq.setParameter("bsTypes", bsTypes);
        iq.setParameter("zoneUuid", msg.getZoneUuid());
        List<ImageVO> vos = iq.getResultList();
        reply.setInventories(ImageInventory.valueOf(vos));
        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    private void getInterdependentL3NetworksByImageUuid(APIGetInterdependentL3NetworksImagesMsg msg) {
        APIGetInterdependentL3NetworkImageReply reply = new APIGetInterdependentL3NetworkImageReply();

        String sql = "select bs.type from BackupStorageVO bs, ImageBackupStorageRefVO ref, BackupStorageZoneRefVO zref" +
                " where bs.uuid = ref.backupStorageUuid and ref.imageUuid = :imgUuid and ref.backupStorageUuid = zref.backupStorageUuid" +
                " and zref.zoneUuid = :zoneUuid";
        TypedQuery<String> bsq = dbf.getEntityManager().createQuery(sql, String.class);
        bsq.setParameter("imgUuid", msg.getImageUuid());
        bsq.setParameter("zoneUuid", msg.getZoneUuid());
        List<String> bssTypes = bsq.getResultList();
        if (bssTypes.isEmpty()) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(
                    String.format("the image[uuid:%s] is not on any backup storage that has been attached to the zone[uuid:%s]",
                            msg.getImageUuid(), msg.getZoneUuid())
            ));
        }

        List<String> possiblePrimaryStorageTypes = new ArrayList<>();
        for (String bsType : bssTypes) {
            List<String> psTypes = hostAllocatorMgr.getPrimaryStorageTypesByBackupStorageTypeFromMetrics(bsType);
            possiblePrimaryStorageTypes.addAll(psTypes);
        }

        sql = "select l3 from L3NetworkVO l3, L2NetworkClusterRefVO l2ref, PrimaryStorageClusterRefVO psref, PrimaryStorageVO ps" +
                " where l3.l2NetworkUuid = l2ref.l2NetworkUuid and l2ref.clusterUuid = psref.clusterUuid" +
                " and psref.primaryStorageUuid = ps.uuid and ps.type in (:psTypes) and ps.zoneUuid = l3.zoneUuid" +
                " and l3.zoneUuid = :zoneUuid group by l3.uuid";
        TypedQuery<L3NetworkVO> l3q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
        l3q.setParameter("psTypes", possiblePrimaryStorageTypes);
        l3q.setParameter("zoneUuid", msg.getZoneUuid());
        List<L3NetworkVO> l3s = l3q.getResultList();
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
                        String.format("zoneUuid must be set because the image[name:%s, uuid:%s] is on multiple backup storage", image.getName(), image.getUuid())
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

                        List<String> clusterUuids = re.getHosts().stream().map(HostInventory::getClusterUuid).collect(Collectors.toList());
                        areply.setClusters(ClusterInventory.valueOf(dbf.listByPrimaryKeys(clusterUuids, ClusterVO.class)));

                        List<String> zoneUuids = re.getHosts().stream().map(HostInventory::getZoneUuid).collect(Collectors.toList());
                        areply.setZones(ZoneInventory.valueOf(dbf.listByPrimaryKeys(zoneUuids, ZoneVO.class)));
                    } else {
                        areply.setHosts(new ArrayList<>());
                        areply.setClusters(new ArrayList<>());
                        areply.setZones(new ArrayList<>());
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
            tagMgr.copySystemTag(instanceOfferingUuid, InstanceOfferingVO.class.getSimpleName(), vo.getUuid(), VmInstanceVO.class.getSimpleName());
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
        cmsg.setDescription(msg.getDescription());
        cmsg.setResourceUuid(msg.getResourceUuid());
        cmsg.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
        cmsg.setStrategy(msg.getStrategy());
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
    }


    @Override
    public boolean start() {
        try {
            createVmFlowChainBuilder();
            populateExtensions();
            installSystemTagValidator();
            installGlobalConfigUpdater();
            setupCanonicalEvents();
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
                            String.format("L3 network[uuid:%s] not found. Please correct your system tag[%s] of static IP", l3Uuid, sysTag)
                    ));
                }

                String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
                if (!NetworkUtils.isIpv4Address(ip)) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("%s is not a valid IPv4 address. Please correct your system tag[%s] of static IP", ip, sysTag)
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
                String sql = "select t from SystemTagVO t, VmInstanceVO vm, VmNicVO nic where t.resourceUuid = vm.uuid and vm.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and t.tag = :sysTag";
                TypedQuery<SystemTagVO> q = dbf.getEntityManager().createQuery(sql, SystemTagVO.class);
                q.setParameter("l3Uuid", l3Uuid);
                q.setParameter("sysTag", tag);
                List<SystemTagVO> vos = q.getResultList();

                if (!vos.isEmpty()) {
                    SystemTagVO sameTag = vos.get(0);
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("conflict hostname in system tag[%s]; there has been a VM[uuid:%s] having hostname[%s] on L3 network[uuid:%s]",
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

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {

            class VmQuota {
                long vmNum;
                long cpuNum;
                long memorySize;
            }

            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
                q.select(AccountVO_.type);
                q.add(AccountVO_.uuid, Op.EQ, msg.getSession().getAccountUuid());
                AccountType type = q.findValue();

                if (type != AccountType.SystemAdmin) {
                    if (msg instanceof APICreateVmInstanceMsg) {
                        check((APICreateVmInstanceMsg) msg, pairs);
                    } else if (msg instanceof APIRecoverVmInstanceMsg) {
                        check((APIRecoverVmInstanceMsg) msg, pairs);
                    } else if (msg instanceof APICreateDataVolumeMsg) {
                        check((APICreateDataVolumeMsg) msg, pairs);
                    } else if (msg instanceof APIRecoverDataVolumeMsg) {
                        check((APIRecoverDataVolumeMsg) msg, pairs);
                    } else if (msg instanceof APIStartVmInstanceMsg) {
                        check((APIStartVmInstanceMsg) msg, pairs);
                    } else if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                } else {
                    if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                }
            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                List<Quota.QuotaUsage> usages = new ArrayList<>();

                VmQuota vmQuota = getUsedVmCpuMemory(accountUuid);
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(VmInstanceConstant.QUOTA_VM_NUM);
                usage.setUsed(vmQuota.vmNum);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VmInstanceConstant.QUOTA_CPU_NUM);
                usage.setUsed(vmQuota.cpuNum);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VmInstanceConstant.QUOTA_VM_MEMORY);
                usage.setUsed(vmQuota.memorySize);
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VolumeConstant.QUOTA_DATA_VOLUME_NUM);
                usage.setUsed(getUsedVolume(accountUuid));
                usages.add(usage);

                usage = new Quota.QuotaUsage();
                usage.setName(VolumeConstant.QUOTA_VOLUME_SIZE);
                usage.setUsed(getUsedVolumeSize(accountUuid));
                usages.add(usage);

                return usages;
            }

            @Transactional(readOnly = true)
            private VmQuota getUsedVmCpuMemory(String accountUUid) {
                VmQuota quota = new VmQuota();

                String sql = "select count(vm), sum(vm.cpuNum), sum(vm.memorySize)" +
                        " from VmInstanceVO vm, AccountResourceRefVO ref where" +
                        " vm.uuid = ref.resourceUuid" +
                        " and ref.accountUuid = :auuid" +
                        " and ref.resourceType = :rtype" +
                        " and vm.state not in (:states)";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("auuid", accountUUid);
                q.setParameter("rtype", VmInstanceVO.class.getSimpleName());
                q.setParameter("states", list(VmInstanceState.Stopped, VmInstanceState.Destroying, VmInstanceState.Destroyed));
                Tuple t = q.getSingleResult();
                Long vnum = t.get(0, Long.class);
                quota.vmNum = vnum == null ? 0 : vnum;
                Long cnum = t.get(1, Long.class);
                quota.cpuNum = cnum == null ? 0 : cnum;
                Long msize = t.get(2, Long.class);
                quota.memorySize = msize == null ? 0 : msize;
                return quota;
            }

            @Transactional(readOnly = true)
            private long getUsedVolume(String accountUuid) {
                String sql = "select count(vol)" +
                        " from VolumeVO vol, AccountResourceRefVO ref " +
                        " where vol.type = :vtype" +
                        " and ref.resourceUuid = vol.uuid " +
                        " and ref.accountUuid = :auuid" +
                        " and ref.resourceType = :rtype" +
                        " and vol.status != :status ";
                TypedQuery<Tuple> volq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                volq.setParameter("auuid", accountUuid);
                volq.setParameter("rtype", VolumeVO.class.getSimpleName());
                volq.setParameter("vtype", VolumeType.Data);
                volq.setParameter("status", VolumeStatus.Deleted);
                Long n = volq.getSingleResult().get(0, Long.class);
                n = n == null ? 0 : n;
                return n;
            }

            @Transactional(readOnly = true)
            private long getUsedVolumeSize(String accountUuid) {
                String sql = "select sum(vol.size) from VolumeVO vol, AccountResourceRefVO ref where " +
                        " ref.resourceUuid = vol.uuid and ref.accountUuid = :auuid and ref.resourceType = :rtype " +
                        " and vol.status != :status";
                TypedQuery<Long> vq = dbf.getEntityManager().createQuery(sql, Long.class);
                vq.setParameter("auuid", accountUuid);
                vq.setParameter("rtype", VolumeVO.class.getSimpleName());
                vq.setParameter("status", VolumeStatus.Deleted);
                Long vsize = vq.getSingleResult();
                vsize = vsize == null ? 0 : vsize;
                return vsize;
            }

            @Transactional(readOnly = true)
            private String getResourceOwnerAccountUuid(String resourceUuid) {
                SimpleQuery<AccountResourceRefVO> q;
                q = dbf.createQuery(AccountResourceRefVO.class);
                q.select(AccountResourceRefVO_.ownerAccountUuid);
                q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, resourceUuid);
                String owner = q.findValue();
                if (owner == null || owner.equals("")) {
                    throw new CloudRuntimeException(String.format("cannot find owner account uuid for resource[uuid:%s]",
                            resourceUuid));
                } else {
                    return owner;
                }
            }

            @Transactional(readOnly = true)
            private void check(APIStartVmInstanceMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceOwnerAccountUuid = getResourceOwnerAccountUuid(msg.getVmInstanceUuid());

                long vmNum = pairs.get(VmInstanceConstant.QUOTA_VM_NUM).getValue();
                long cpuNum = pairs.get(VmInstanceConstant.QUOTA_CPU_NUM).getValue();
                long memory = pairs.get(VmInstanceConstant.QUOTA_VM_MEMORY).getValue();

                VmQuota vmQuota = getUsedVmCpuMemory(resourceOwnerAccountUuid);

                if (vmQuota.vmNum + 1 > vmNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. Current account is [uuid: %s]. " +
                                            "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, resourceOwnerAccountUuid,
                                    VmInstanceConstant.QUOTA_VM_NUM, vmNum)
                    ));
                }

                VmInstanceVO vm = dbf.getEntityManager().find(VmInstanceVO.class, msg.getUuid());
                int cpuNumAsked = vm.getCpuNum();
                long memoryAsked = vm.getMemorySize();
                if (vmQuota.cpuNum + cpuNumAsked > cpuNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. Current account is [uuid: %s]. " +
                                            "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, resourceOwnerAccountUuid,
                                    VmInstanceConstant.QUOTA_CPU_NUM, cpuNum)
                    ));
                }

                if (vmQuota.memorySize + memoryAsked > memory) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. Current account is [uuid: %s]. " +
                                            "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, resourceOwnerAccountUuid,
                                    VmInstanceConstant.QUOTA_VM_MEMORY, memory)
                    ));
                }
            }

            @Transactional(readOnly = true)
            private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
                SimpleQuery<AccountVO> q1 = dbf.createQuery(AccountVO.class);
                q1.select(AccountVO_.type);
                q1.add(AccountVO_.uuid, Op.EQ, msg.getSession().getAccountUuid());
                AccountType type = q1.findValue();
                if (type == AccountType.SystemAdmin && (pairs == null || pairs.size() == 0)) {
                    logger.debug("APIChangeResourceOwnerMsg:(pairs == null || pairs.size() == 0)." +
                            "Skip quota check for being called by QuotaChecker with admin account session." +
                            "Another quota check would be executed by message interceptor.");
                    return;
                }

                SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
                q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, msg.getResourceUuid());
                AccountResourceRefVO accResRefVO = q.find();

                String resourceOriginalOwnerAccountUuid = accResRefVO.getOwnerAccountUuid();
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
                if (resourceTargetOwnerAccountUuid.equals(resourceOriginalOwnerAccountUuid)) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_INVALID_OP,
                            String.format("Invalid ChangeResourceOwner operation." +
                                            "Original owner is the same as target owner." +
                                            "Current account is [uuid: %s]." +
                                            "The resource target owner account[uuid: %s]." +
                                            "The resource original owner account[uuid:%s].",
                                    currentAccountUuid, resourceTargetOwnerAccountUuid, resourceOriginalOwnerAccountUuid)
                    ));
                }

                if (accResRefVO.getResourceType().equals(VolumeVO.class.getSimpleName())) {
                    String volumeUuid = msg.getResourceUuid();

                    long volNum = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                    long volSize = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();

                    // check data volume num
                    long n = getUsedVolume(resourceTargetOwnerAccountUuid);
                    if (n + 1 > volNum) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. Current account is [uuid: %s]. " +
                                                "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        currentAccountUuid, resourceTargetOwnerAccountUuid,
                                        VolumeConstant.QUOTA_DATA_VOLUME_NUM, volNum)
                        ));
                    }

                    // check data volume size
                    long requiredVolSize;
                    String sql = "select size from VolumeVO where uuid = :uuid ";
                    TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                    dq.setParameter("uuid", volumeUuid);
                    Long dsize = dq.getSingleResult();
                    dsize = dsize == null ? 0 : dsize;
                    requiredVolSize = dsize;

                    long vsize = getUsedVolumeSize(resourceTargetOwnerAccountUuid);
                    if (vsize + requiredVolSize > volSize) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. Current account is [uuid: %s]. " +
                                                "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        currentAccountUuid, resourceTargetOwnerAccountUuid,
                                        VolumeConstant.QUOTA_VOLUME_SIZE, volSize)
                        ));
                    }
                } else if (accResRefVO.getResourceType().equals(VmInstanceVO.class.getSimpleName())) {
                    long vmNum = pairs.get(VmInstanceConstant.QUOTA_VM_NUM).getValue();
                    long cpuNum = pairs.get(VmInstanceConstant.QUOTA_CPU_NUM).getValue();
                    long memory = pairs.get(VmInstanceConstant.QUOTA_VM_MEMORY).getValue();
                    String vmInstanceUuid = msg.getResourceUuid();

                    VmQuota vmQuota = getUsedVmCpuMemory(resourceTargetOwnerAccountUuid);

                    if (vmQuota.vmNum + 1 > vmNum) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. Current account is [uuid: %s]. " +
                                                "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        currentAccountUuid, resourceTargetOwnerAccountUuid,
                                        VmInstanceConstant.QUOTA_VM_NUM, vmNum)
                        ));
                    }

                    VmInstanceVO vm = dbf.getEntityManager().find(VmInstanceVO.class, vmInstanceUuid);
                    int cpuNumAsked = vm.getCpuNum();
                    long memoryAsked = vm.getMemorySize();
                    if (vmQuota.cpuNum + cpuNumAsked > cpuNum) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. Current account is [uuid: %s]. " +
                                                "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        currentAccountUuid, resourceTargetOwnerAccountUuid,
                                        VmInstanceConstant.QUOTA_CPU_NUM, cpuNum)
                        ));
                    }

                    if (vmQuota.memorySize + memoryAsked > memory) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. Current account is [uuid: %s]. " +
                                                "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        currentAccountUuid, resourceTargetOwnerAccountUuid,
                                        VmInstanceConstant.QUOTA_VM_MEMORY, memory)
                        ));
                    }
                }

            }

            @Transactional(readOnly = true)
            private void checkVolumeQuota(String currentAccountUuid, String resourceOwnerAccountUuid,
                                          String volumeUuid, Map<String, Quota.QuotaPair> pairs) {
                long volNum = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long volSize = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();

                // check data volume num
                long n = getUsedVolume(resourceOwnerAccountUuid);
                if (n + 1 > volNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. Current account is [uuid: %s]. " +
                                            "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, resourceOwnerAccountUuid,
                                    VolumeConstant.QUOTA_DATA_VOLUME_NUM, volNum)
                    ));
                }

                // check data volume size
                long requiredVolSize;
                String sql = "select size from VolumeVO where uuid = :uuid ";
                TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                dq.setParameter("uuid", volumeUuid);
                Long dsize = dq.getSingleResult();
                dsize = dsize == null ? 0 : dsize;
                requiredVolSize = dsize;

                long vsize = getUsedVolumeSize(resourceOwnerAccountUuid);
                if (vsize + requiredVolSize > volSize) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. Current account is [uuid: %s]. " +
                                            "The resource owner account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    currentAccountUuid, resourceOwnerAccountUuid,
                                    VolumeConstant.QUOTA_VOLUME_SIZE, volSize)
                    ));
                }
            }


            private void check(APIRecoverDataVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceOwnerAccountUuid = getResourceOwnerAccountUuid(msg.getVolumeUuid());

                checkVolumeQuota(currentAccountUuid, resourceOwnerAccountUuid, msg.getVolumeUuid(), pairs);

            }

            @Transactional(readOnly = true)
            private void check(APICreateDataVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                long volNum = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long volSize = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();

                // check data volume num
                long n = getUsedVolume(msg.getSession().getAccountUuid());
                if (n + 1 > volNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VolumeConstant.QUOTA_DATA_VOLUME_NUM, volNum)
                    ));
                }

                // check data volume size
                long requiredVolSize;
                String sql = "select diskSize from DiskOfferingVO where uuid = :uuid ";
                TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                dq.setParameter("uuid", msg.getDiskOfferingUuid());
                Long dsize = dq.getSingleResult();
                dsize = dsize == null ? 0 : dsize;
                requiredVolSize = dsize;

                long vsize = getUsedVolumeSize(msg.getSession().getAccountUuid());
                if (vsize + requiredVolSize > volSize) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VolumeConstant.QUOTA_VOLUME_SIZE, volSize)
                    ));
                }
            }

            @Transactional(readOnly = true)
            private void check(APIRecoverVmInstanceMsg msg, Map<String, QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();

                SimpleQuery<VolumeVO> q1 = dbf.createQuery(VolumeVO.class);
                q1.select(VolumeVO_.uuid);
                q1.add(VolumeVO_.type, Op.EQ, VolumeType.Root);
                q1.add(VolumeVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
                String volumeUuid = q1.findValue();
                String resourceOwnerAccountUuid = getResourceOwnerAccountUuid(msg.getVmInstanceUuid());

                checkVolumeQuota(currentAccountUuid, resourceOwnerAccountUuid, volumeUuid, pairs);
            }

            @Transactional(readOnly = true)
            private void check(APICreateVmInstanceMsg msg, Map<String, QuotaPair> pairs) {
                long vmNum = pairs.get(VmInstanceConstant.QUOTA_VM_NUM).getValue();
                long cpuNum = pairs.get(VmInstanceConstant.QUOTA_CPU_NUM).getValue();
                long memory = pairs.get(VmInstanceConstant.QUOTA_VM_MEMORY).getValue();
                long volNum = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long volSize = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();

                VmQuota vmQuota = getUsedVmCpuMemory(msg.getSession().getAccountUuid());

                if (vmQuota.vmNum + 1 > vmNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VmInstanceConstant.QUOTA_VM_NUM, vmNum)
                    ));
                }

                String sql = "select i.cpuNum, i.memorySize from InstanceOfferingVO i where i.uuid = :uuid";
                TypedQuery<Tuple> iq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                iq.setParameter("uuid", msg.getInstanceOfferingUuid());
                Tuple it = iq.getSingleResult();
                int cpuNumAsked = it.get(0, Integer.class);
                long memoryAsked = it.get(1, Long.class);

                if (vmQuota.cpuNum + cpuNumAsked > cpuNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VmInstanceConstant.QUOTA_CPU_NUM, cpuNum)
                    ));
                }

                if (vmQuota.memorySize + memoryAsked > memory) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VmInstanceConstant.QUOTA_VM_MEMORY, memory)
                    ));
                }

                // check data volume num
                if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
                    long n = getUsedVolume(msg.getSession().getAccountUuid());

                    if (n + msg.getDataDiskOfferingUuids().size() > volNum) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        msg.getSession().getAccountUuid(), VolumeConstant.QUOTA_DATA_VOLUME_NUM, volNum)
                        ));
                    }
                }

                long requiredVolSize = 0;

                sql = "select img.size, img.mediaType from ImageVO img where img.uuid = :iuuid";
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
                    requiredVolSize += imgSize;
                } else if (imgType == ImageMediaType.ISO) {
                    diskOfferingUuids.add(msg.getRootDiskOfferingUuid());
                }
                if (!diskOfferingUuids.isEmpty()) {
                    sql = "select sum(d.diskSize) from DiskOfferingVO d where d.uuid in (:uuids)";
                    TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                    dq.setParameter("uuids", diskOfferingUuids);
                    Long dsize = dq.getSingleResult();
                    dsize = dsize == null ? 0 : dsize;
                    requiredVolSize += dsize;
                }

                long vsize = getUsedVolumeSize(msg.getSession().getAccountUuid());
                if (vsize + requiredVolSize > volSize) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VolumeConstant.QUOTA_VOLUME_SIZE, volSize)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        QuotaPair p = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_NUM);
        p.setValue(20);
        quota.addPair(p);

        p = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_CPU_NUM);
        p.setValue(80);
        quota.addPair(p);

        p = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_MEMORY);
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

        quota.addMessageNeedValidation(APICreateVmInstanceMsg.class);
        quota.addMessageNeedValidation(APIRecoverVmInstanceMsg.class);
        quota.addMessageNeedValidation(APICreateDataVolumeMsg.class);
        quota.addMessageNeedValidation(APIRecoverDataVolumeMsg.class);
        quota.addMessageNeedValidation(APIStartVmInstanceMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
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
        List<String> ret = new ArrayList<String>();
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
                    logger.warn(String.format("the vm[uuid:%s] is in Unknown state, but its hostUuid is null, we cannot check its" +
                            " real state", vmUuid));
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
            TimeBasedGCEphemeralContext<Void> context = new TimeBasedGCEphemeralContext<Void>();
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
                List<Tuple> ret = new ArrayList<Tuple>();
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
                                logger.warn(String.format("failed to expunge the vm[uuid:%s], %s", msg.getVmInstanceUuid(), r.getError()));
                            } else {
                                logger.debug(String.format("successfully expunged the vm[uuid:%s]", msg.getVmInstanceUuid()));
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

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.rootVolumeUuid);
        q.add(VmInstanceVO_.uuid, Op.EQ, ref.getResourceUuid());
        String rootVolumeUuid = q.findValue();
        if (rootVolumeUuid == null) {
            return;
        }

        acntMgr.changeResourceOwner(rootVolumeUuid, newOwnerUuid);
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

    public String preDestroyVm(VmInstanceInventory inv) {
        return null;
    }

    public void beforeDestroyVm(VmInstanceInventory inv) {
        logger.debug(String.format("will pause scheduler before destroy vm %s", inv.getUuid()));
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.add(SchedulerVO_.targetResourceUuid, Op.EQ, inv.getUuid());
        q.select(SchedulerVO_.uuid);
        List<String> uuids = q.listValue();
        for (String uuid : uuids) {
            schedulerFacade.pauseSchedulerJob(uuid);
        }

    }

    public void afterDestroyVm(VmInstanceInventory vm) {

    }

    public void failedToDestroyVm(VmInstanceInventory vm, ErrorCode reason) {

    }

    public void preRecoverVm(VmInstanceInventory vm) {

    }

    public void beforeRecoverVm(VmInstanceInventory vm) {

    }

    public void afterRecoverVm(VmInstanceInventory vm) {
        logger.debug(String.format("will resume scheduler after recover vm %s", vm.getUuid()));
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.add(SchedulerVO_.targetResourceUuid, Op.EQ, vm.getUuid());
        logger.debug(String.format("target resource uuid is %s", vm.getUuid()));
        q.select(SchedulerVO_.uuid);
        List<String> uuids = q.listValue();
        for (String uuid : uuids) {
            logger.debug(String.format("scheduler uuid is %s", uuid));
            schedulerFacade.resumeSchedulerJob(uuid);
        }

    }

    public void vmBeforeExpunge(VmInstanceInventory inv) {
        logger.debug(String.format("will delete scheduler before expunge vm"));
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.add(SchedulerVO_.targetResourceUuid, Op.EQ, inv.getUuid());
        q.select(SchedulerVO_.uuid);
        List<String> uuids = q.listValue();
        for (String uuid : uuids) {
            schedulerFacade.deleteSchedulerJob(uuid);
        }
    }
}
