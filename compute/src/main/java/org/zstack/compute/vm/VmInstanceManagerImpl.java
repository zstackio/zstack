package org.zstack.compute.vm;

import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostStatusChangeNotifyPoint;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.CheckQuotaForApiMessage;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.search.SearchOp;
import org.zstack.header.tag.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicVmState;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.search.SearchQuery;
import org.zstack.tag.SystemTag;
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
import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

public class VmInstanceManagerImpl extends AbstractService implements VmInstanceManager, HostStatusChangeNotifyPoint, ReportQuotaExtensionPoint {
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
    private FlowChainBuilder createVmFlowBuilder;
    private FlowChainBuilder stopVmFlowBuilder;
    private FlowChainBuilder rebootVmFlowBuilder;
    private FlowChainBuilder startVmFlowBuilder;
    private FlowChainBuilder destroyVmFlowBuilder;
    private FlowChainBuilder migrateVmFlowBuilder;
    private FlowChainBuilder attachVolumeFlowBuilder;
    private FlowChainBuilder attachIsoFlowBuilder;
    private FlowChainBuilder detachIsoFlowBuilder;
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

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
            bus.replyErrorByMessageType((Message)msg, err);
            return;
        }

        VmInstanceFactory factory = getVmInstanceFactory(VmInstanceType.valueOf(vo.getType()));
        VmInstance vm = factory.getVmInstance(vo);
        vm.handleMessage((Message)msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VmInstanceMessage) {
            passThrough((VmInstanceMessage)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVmInstanceMsg) {
            handle((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIListVmInstanceMsg) {
            handle((APIListVmInstanceMsg)msg);
        } else if (msg instanceof APISearchVmInstanceMsg) {
            handle((APISearchVmInstanceMsg) msg);
        } else if (msg instanceof APIGetVmInstanceMsg) {
            handle((APIGetVmInstanceMsg) msg);
        } else if (msg instanceof APIListVmNicMsg) {
            handle((APIListVmNicMsg) msg);
        } else if (msg instanceof VmInstanceMessage) {
            passThrough((VmInstanceMessage)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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


    private void handle(final APICreateVmInstanceMsg msg) {
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
        vo.setInstanceOfferingUuid(msg.getInstanceOfferingUuid());
        vo.setState(VmInstanceState.Created);
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setInternalId(dbf.generateSequenceNumber(VmInstanceSequenceNumberVO.class));
        vo.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());

        SimpleQuery<ImageVO> imgq = dbf.createQuery(ImageVO.class);
        imgq.select(ImageVO_.platform);
        imgq.add(ImageVO_.uuid, Op.EQ, msg.getImageUuid());
        ImagePlatform platform = imgq.findValue();
        vo.setPlatform(platform.toString());

        InstanceOfferingVO iovo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        vo.setCpuNum(iovo.getCpuNum());
        vo.setCpuSpeed(iovo.getCpuSpeed());
        vo.setMemorySize(iovo.getMemorySize());
        vo.setAllocatorStrategy(iovo.getAllocatorStrategy());

        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), VmInstanceVO.class);

        String vmType = msg.getType() == null ? VmInstanceConstant.USER_VM_TYPE : msg.getType();
        VmInstanceType type = VmInstanceType.valueOf(vmType);
        VmInstanceFactory factory = getVmInstanceFactory(type);
        vo = factory.createVmInstance(vo, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), VmInstanceVO.class.getSimpleName());
        tagMgr.copySystemTag(iovo.getUuid(), InstanceOfferingVO.class.getSimpleName(), vo.getUuid(), VmInstanceVO.class.getSimpleName());

        StartNewCreatedVmInstanceMsg smsg = new StartNewCreatedVmInstanceMsg();
        smsg.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        smsg.setL3NetworkUuids(msg.getL3NetworkUuids());
        smsg.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        smsg.setVmInstanceInventory(VmInstanceInventory.valueOf(vo));
        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, vo.getUuid());
        bus.send(smsg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                try {
                    APICreateVmInstanceEvent evt = new APICreateVmInstanceEvent(msg.getId());
                    if (reply.isSuccess()) {
                        StartNewCreatedVmInstanceReply r = (StartNewCreatedVmInstanceReply) reply;
                        evt.setInventory(r.getVmInventory());
                    } else {
                        evt.setErrorCode(reply.getError());
                        evt.setSuccess(false);
                    }
                    bus.publish(evt);
                } catch (Exception e) {
                    bus.logExceptionWithMessageDump(msg, e);
                    bus.replyErrorByMessageType(msg, e);
                }
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
            return true;
        } catch (Exception e) {
            throw new CloudConfigureFailException(VmInstanceManagerImpl.class, e.getMessage(), e);
        }
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
                        if (++ hostnameCount > 1) {
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
                }
            }
        }

        HostNameValidator hostnameValidator = new HostNameValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), hostnameValidator);
        VmSystemTags.HOSTNAME.installValidator(hostnameValidator);

        VmSystemTags.STATIC_IP.installLifeCycleListener(new AbstractSystemTagLifeCycleListener() {
            private void markNicToReleaseAndAcquireNewIp(SystemTagInventory tag) {
                String l3NetworkUuid = VmSystemTags.STATIC_IP.getTokenByTag(tag.getTag(), VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
                q.add(VmNicVO_.vmInstanceUuid, Op.EQ, tag.getResourceUuid());
                q.add(VmNicVO_.l3NetworkUuid, Op.EQ, l3NetworkUuid);
                VmNicVO nic = q.find();
                if (nic == null) {
                    logger.warn(String.format("cannot find nic[vm uuid:%s, l3 uuid:%s] for updating static ip system tag[uuid:%s]",
                            tag.getResourceUuid(), l3NetworkUuid, tag.getUuid()));
                } else {
                    nic.setMetaData(VmInstanceConstant.NIC_META_RELEASE_IP_AND_ACQUIRE_NEW);
                    dbf.update(nic);
                }
            }

            @Override
            public void tagCreated(SystemTagInventory tag) {
                markNicToReleaseAndAcquireNewIp(tag);
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                markNicToReleaseAndAcquireNewIp(newTag);
            }
        });
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
    public void notifyHostConnectionStateChange(HostInventory host, HostStatus previousState, HostStatus currentState) {
        if (currentState == HostStatus.Disconnected) {
            putVmToUnknownState(host.getUuid());
        }
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

    @Override
    public List<Quota> reportQuota() {
        CheckQuotaForApiMessage checker = new CheckQuotaForApiMessage() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (msg instanceof APICreateVmInstanceMsg) {
                    check((APICreateVmInstanceMsg) msg, pairs);
                } 
            }

            @Transactional(readOnly = true)
            private void check(APICreateVmInstanceMsg msg, Map<String, QuotaPair> pairs) {
                long vmNum = pairs.get(VmInstanceConstant.QUOTA_VM_NUM).getValue();
                long cpuNum = pairs.get(VmInstanceConstant.QUOTA_CPU_NUM).getValue();
                long memory = pairs.get(VmInstanceConstant.QUOTA_VM_MEMORY).getValue();
                long volNum = pairs.get(VolumeConstant.QUOTA_DATA_VOLUME_NUM).getValue();
                long volSize = pairs.get(VolumeConstant.QUOTA_VOLUME_SIZE).getValue();

                String sql = "select count(vm), sum(vm.cpuNum), sum(vm.memorySize) from VmInstanceVO vm, AccountResourceRefVO ref where" +
                        " vm.uuid = ref.resourceUuid and ref.accountUuid = :auuid and ref.resourceType = :rtype";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("auuid", msg.getSession().getAccountUuid());
                q.setParameter("rtype", VmInstanceVO.class.getSimpleName());
                Tuple t = q.getSingleResult();
                Long vnum = t.get(0, Long.class);
                vnum = vnum == null ? 0 : vnum;
                Long cnum = t.get(1, Long.class);
                cnum = cnum == null ? 0 : cnum;
                Long msize = t.get(2, Long.class);
                msize = msize == null ? 0 : msize;

                if (vnum + 1 > vmNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VmInstanceConstant.QUOTA_VM_NUM, vmNum)
                    ));
                }

                sql = "select i.cpuNum, i.memorySize from InstanceOfferingVO i where i.uuid = :uuid";
                TypedQuery<Tuple> iq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                iq.setParameter("uuid", msg.getInstanceOfferingUuid());
                Tuple it = iq.getSingleResult();
                int cpuNumAsked = it.get(0, Integer.class);
                long memoryAsked = it.get(1, Long.class);

                if (cnum + cpuNumAsked > cpuNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VmInstanceConstant.QUOTA_CPU_NUM, cpuNum)
                    ));
                }

                if (msize + memoryAsked > memory) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VmInstanceConstant.QUOTA_VM_MEMORY, memory)
                    ));
                }

                // check data volume num
                if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
                    sql = "select count(vol) from VolumeVO vol, AccountResourceRefVO ref where vol.type = :vtype and ref.resourceUuid = vol.uuid and ref.accountUuid = :auuid and ref.resourceType = :rtype";
                    TypedQuery<Tuple> volq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    volq.setParameter("auuid", msg.getSession().getAccountUuid());
                    volq.setParameter("rtype", VolumeVO.class.getSimpleName());
                    volq.setParameter("vtype", VolumeType.Data);
                    Long n = volq.getSingleResult().get(0, Long.class);
                    n = n == null ? 0 : n;

                    if (n + msg.getDataDiskOfferingUuids().size() > volNum) {
                        throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                                String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                        msg.getSession().getAccountUuid(), VolumeConstant.QUOTA_DATA_VOLUME_NUM, memory)
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

                List<String> diskOfferingUuids = new ArrayList<String>();
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

                sql = "select sum(vol.size) from VolumeVO vol, AccountResourceRefVO ref where" +
                        " ref.resourceUuid = vol.uuid and ref.accountUuid = :auuid and ref.resourceType = :rtype";
                TypedQuery<Long> vq = dbf.getEntityManager().createQuery(sql, Long.class);
                vq.setParameter("auuid", msg.getSession().getAccountUuid());
                vq.setParameter("rtype", VolumeVO.class.getSimpleName());
                Long vsize = vq.getSingleResult();
                vsize = vsize == null ? 0 : vsize;

                if (vsize + requiredVolSize > volSize) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VolumeConstant.QUOTA_VOLUME_SIZE, volSize)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        QuotaPair p  = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_NUM);
        p.setValue(20);
        quota.addPair(p);

        p  = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_CPU_NUM);
        p.setValue(80);
        quota.addPair(p);

        p  = new QuotaPair();
        p.setName(VmInstanceConstant.QUOTA_VM_MEMORY);
        p.setValue(SizeUnit.GIGABYTE.toByte(80));
        quota.addPair(p);

        p  = new QuotaPair();
        p.setName(VolumeConstant.QUOTA_DATA_VOLUME_NUM);
        p.setValue(40);
        quota.addPair(p);

        p  = new QuotaPair();
        p.setName(VolumeConstant.QUOTA_VOLUME_SIZE);
        p.setValue(SizeUnit.TERABYTE.toByte(10));
        quota.addPair(p);

        quota.setMessageNeedValidation(APICreateVmInstanceMsg.class);
        quota.setChecker(checker);

        return list(quota);
    }
}
