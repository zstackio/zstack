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
import org.zstack.core.workflow.FlowChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostStatusChangeNotifyPoint;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.search.SearchOp;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.*;
import org.zstack.header.vm.ChangeVmMetaDataMsg.AtomicVmState;
import org.zstack.identity.AccountManager;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

public class VmInstanceManagerImpl extends AbstractService implements VmInstanceManager, HostStatusChangeNotifyPoint {
    private static final CLogger logger = Utils.getLogger(VmInstanceManagerImpl.class);
    private Map<String, VmInstanceFactory> vmInstanceFactories = Collections.synchronizedMap(new HashMap<String, VmInstanceFactory>());
    private List<String> createVmWorkFlowElements;
    private List<String> stopVmWorkFlowElements;
    private List<String> rebootVmWorkFlowElements;
    private List<String> startVmWorkFlowElements;
    private List<String> destroyVmWorkFlowElements;
    private List<String> migrateVmWorkFlowElements;
    private List<String> attachVolumeWorkFlowElements;
    private FlowChainBuilder createVmFlowBuilder;
    private FlowChainBuilder stopVmFlowBuilder;
    private FlowChainBuilder rebootVmFlowBuilder;
    private FlowChainBuilder startVmFlowBuilder;
    private FlowChainBuilder destroyVmFlowBuilder;
    private FlowChainBuilder migrateVmFlowBuilder;
    private FlowChainBuilder attachVolumeFlowBuilder;
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
                    }
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
    protected void putVmToUnknownState(String hostUuid) {
        SimpleQuery<VmInstanceVO> query = dbf.createQuery(VmInstanceVO.class);
        query.select(VmInstanceVO_.uuid, VmInstanceVO_.state);
        query.add(VmInstanceVO_.hostUuid, Op.EQ, hostUuid);
        List<Tuple> tss = query.listTuple();
        for (Tuple ts : tss) {
            ChangeVmMetaDataMsg msg = new ChangeVmMetaDataMsg();
            msg.setVmInstanceUuid(ts.get(0, String.class));
            AtomicVmState s = new AtomicVmState();
            s.setExpected(ts.get(1, VmInstanceState.class));
            s.setValue(VmInstanceState.Unknown);
            msg.setState(s);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, ts.get(0, String.class));
            bus.send(msg);
        }
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
}
