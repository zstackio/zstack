package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowChainProcessor;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class EipManagerImpl extends AbstractService implements EipManager, VipReleaseExtensionPoint,
        AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, VmPreAttachL3NetworkExtensionPoint, VmIpChangedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(EipManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private VipManager vipMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ErrorFacade errf;

    private Map<String, EipBackend> backends = new HashMap<String, EipBackend>();
    private List<String> createEipFlowNames;
    private List<String> removeEipFlowNames;
    private List<String> attachEipFlowNames;
    private List<String> detachEipFlowNames;
    private FlowChainBuilder createEipFlowBuilder;
    private FlowChainBuilder removeEipFlowBuilder;
    private FlowChainBuilder attachEipFlowBuilder;
    private FlowChainBuilder detachEipFlowBuilder;


    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateEipMsg) {
            handle((APICreateEipMsg) msg);
        } else if (msg instanceof APIDeleteEipMsg) {
            handle((APIDeleteEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            handle((APIAttachEipMsg) msg);
        } else if (msg instanceof APIDetachEipMsg) {
            handle((APIDetachEipMsg) msg);
        } else if (msg instanceof APIChangeEipStateMsg) {
            handle((APIChangeEipStateMsg) msg);
        } else if (msg instanceof APIGetEipAttachableVmNicsMsg) {
            handle((APIGetEipAttachableVmNicsMsg) msg);
        } else if (msg instanceof APIUpdateEipMsg) {
            handle((APIUpdateEipMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateEipMsg msg) {
        EipVO vo = dbf.findByUuid(msg.getUuid(), EipVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateEipEvent evt = new APIUpdateEipEvent(msg.getId());
        evt.setInventory(EipInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Transactional(readOnly = true)
    private List<VmNicInventory> getAttachableVmNicForEip(String eipUuid, String vipUuid) {
        String zoneUuid = null;
        if (eipUuid != null) {
            String sql = "select l3.zoneUuid, vip.uuid from L3NetworkVO l3, VipVO vip, EipVO eip where l3.uuid = vip.l3NetworkUuid and eip.vipUuid = vip.uuid and eip.uuid = :eipUuid";
            TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
            q.setParameter("eipUuid", eipUuid);
            Tuple t = q.getSingleResult();
            zoneUuid = t.get(0, String.class);
            vipUuid = t.get(1, String.class);
        } else {
            String sql = "select l3.zoneUuid from L3NetworkVO l3, VipVO vip where l3.uuid = vip.l3NetworkUuid and vip.uuid = :vipUuid";
            TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
            q.setParameter("vipUuid", vipUuid);
            zoneUuid = q.getSingleResult();
        }


        String sql = "select l3.uuid from L3NetworkVO l3, VipVO vip, NetworkServiceL3NetworkRefVO ref where l3.system = :system and l3.uuid != vip.l3NetworkUuid and l3.uuid = ref.l3NetworkUuid and ref.networkServiceType = :nsType and l3.zoneUuid = :zoneUuid and vip.uuid = :vipUuid";
        TypedQuery<String> l3q = dbf.getEntityManager().createQuery(sql, String.class);
        l3q.setParameter("vipUuid", vipUuid);
        l3q.setParameter("system", false);
        l3q.setParameter("zoneUuid", zoneUuid);
        l3q.setParameter("nsType", EipConstant.EIP_NETWORK_SERVICE_TYPE);
        List<String> l3Uuids = l3q.getResultList();
        if (l3Uuids.isEmpty()) {
            return new ArrayList<VmNicInventory>();
        }

        sql = "select nic from VmNicVO nic, VmInstanceVO vm where nic.l3NetworkUuid in (:l3Uuids) and nic.vmInstanceUuid = vm.uuid and vm.type = :vmType and vm.state in (:vmStates) and nic.uuid not in (select eip.vmNicUuid from EipVO eip where eip.vmNicUuid is not null)";
        TypedQuery<VmNicVO> nq = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        nq.setParameter("l3Uuids", l3Uuids);
        nq.setParameter("vmType", VmInstanceConstant.USER_VM_TYPE);
        nq.setParameter("vmStates", Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped));
        List<VmNicVO> nics = nq.getResultList();
        return VmNicInventory.valueOf(nics);
    }

    private void handle(APIGetEipAttachableVmNicsMsg msg) {
        APIGetEipAttachableVmNicsReply reply = new APIGetEipAttachableVmNicsReply();
        List<VmNicInventory> nics = getAttachableVmNicForEip(msg.getEipUuid(), msg.getVipUuid());
        reply.setInventories(nics);
        bus.reply(msg, reply);
    }

    private void handle(APIChangeEipStateMsg msg) {
        EipVO eip = dbf.findByUuid(msg.getUuid(), EipVO.class);
        eip.setState(eip.getState().nextState(EipStateEvent.valueOf(msg.getStateEvent())));
        eip = dbf.updateAndRefresh(eip);

        APIChangeEipStateEvent evt = new APIChangeEipStateEvent(msg.getId());
        evt.setInventory(EipInventory.valueOf(eip));
        bus.publish(evt);
    }

    private void handle(APIDetachEipMsg msg) {
        final APIDetachEipEvent evt = new APIDetachEipEvent(msg.getId());
        final EipVO vo = dbf.findByUuid(msg.getUuid(), EipVO.class);

        VmNicVO nicvo = dbf.findByUuid(vo.getVmNicUuid(), VmNicVO.class);
        VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);
        VipVO vipvo = dbf.findByUuid(vo.getVipUuid(), VipVO.class);
        VipInventory vipInventory = VipInventory.valueOf(vipvo);

        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);
        EipStruct struct = new EipStruct();
        struct.setVip(vipInventory);
        struct.setNic(nicInventory);
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        detachEipAndUpdateDb(struct, providerType.toString(), new Completion() {
            @Override
            public void success() {
                evt.setInventory(EipInventory.valueOf(dbf.reload(vo)));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIAttachEipMsg msg) {
        final APIAttachEipEvent evt = new APIAttachEipEvent(msg.getId());
        final EipVO vo = dbf.findByUuid(msg.getEipUuid(), EipVO.class);

        VmNicVO nicvo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        final VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);
        VipVO vipvo = dbf.findByUuid(vo.getVipUuid(), VipVO.class);
        VipInventory vipInventory = VipInventory.valueOf(vipvo);

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, nicvo.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (VmInstanceState.Running != state) {
            vo.setVmNicUuid(nicInventory.getUuid());
            vo.setGuestIp(nicvo.getIp());
            EipVO evo = dbf.updateAndRefresh(vo);
            evt.setInventory(EipInventory.valueOf(evo));
            bus.publish(evt);
            return;
        }

        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);
        EipStruct struct = new EipStruct();
        struct.setNic(nicInventory);
        struct.setVip(vipInventory);
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        attachEip(struct, providerType.toString(), new Completion(msg) {
            @Override
            public void success() {
                vo.setVmNicUuid(nicInventory.getUuid());
                vo.setGuestIp(nicInventory.getIp());
                EipVO evo = dbf.updateAndRefresh(vo);
                evt.setInventory(EipInventory.valueOf(evo));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDeleteEipMsg msg) {
        final APIDeleteEipEvent evt = new APIDeleteEipEvent(msg.getId());
        final EipVO vo = dbf.findByUuid(msg.getUuid(), EipVO.class);
        VipVO vipvo = dbf.findByUuid(vo.getVipUuid(), VipVO.class);
        VipInventory vipInventory = VipInventory.valueOf(vipvo);

        if (vo.getVmNicUuid() == null) {
            vipMgr.unlockVip(vipInventory);
            dbf.remove(vo);
            bus.publish(evt);
            return;
        }
        VmNicVO nicvo = dbf.findByUuid(vo.getVmNicUuid(), VmNicVO.class);
        VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);

        EipStruct struct = new EipStruct();
        struct.setNic(nicInventory);
        struct.setVip(vipInventory);
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);

        FlowChain chain = removeEipFlowBuilder.build();
        chain.setName(String.format("delete-eip-vmNic-%s-vip-%s", nicvo.getUuid(), vipvo.getUuid()));
        chain.getData().put(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString(), providerType.toString());
        chain.getData().put(EipConstant.Params.EIP_STRUCT.toString(), struct);
        chain.getData().put(EipConstant.Params.NEED_UNLOCK_VIP.toString(), true);
        chain.getData().put(VipConstant.Params.VIP_SERVICE_PROVIDER_TYPE.toString(), providerType);
        chain.getData().put(VipConstant.Params.VIP.toString(), vipInventory);
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                dbf.remove(vo);
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setErrorCode(errCode);
                bus.publish(evt);
            }
        }).start();

    }

    private void handle(APICreateEipMsg msg) {
        final APICreateEipEvent evt = new APICreateEipEvent(msg.getId());

        EipVO vo = new EipVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setVipUuid(msg.getVipUuid());

        SimpleQuery<VipVO> vipq = dbf.createQuery(VipVO.class);
        vipq.select(VipVO_.ip);
        vipq.add(VipVO_.uuid, Op.EQ, msg.getVipUuid());
        String vipIp = vipq.findValue();
        vo.setVipIp(vipIp);

        vo.setVmNicUuid(msg.getVmNicUuid());
        vo.setState(EipState.Enabled);
        vo = dbf.persistAndRefresh(vo);

        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), EipVO.class);
        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), EipVO.class.getSimpleName());

        VipVO vipvo = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        final VipInventory vipInventory = VipInventory.valueOf(vipvo);

        if (vo.getVmNicUuid() == null) {
            vipMgr.lockVip(vipInventory, EipConstant.EIP_NETWORK_SERVICE_TYPE);
            evt.setInventory(EipInventory.valueOf(vo));
            logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s, ip:%s]",
                    vo.getUuid(), vo.getName(), vipInventory.getUuid(), vipInventory.getIp()));
            bus.publish(evt);
            return;
        }

        VmNicVO nicvo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        vo.setGuestIp(nicvo.getIp());
        vo = dbf.updateAndRefresh(vo);
        final EipInventory retinv = EipInventory.valueOf(vo);

        final VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);
        L3NetworkVO l3vo = dbf.findByUuid(nicvo.getL3NetworkUuid(), L3NetworkVO.class);
        L3NetworkInventory l3NetworkInventory = L3NetworkInventory.valueOf(l3vo);

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, nicvo.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (state != VmInstanceState.Running) {
            vipMgr.lockVip(vipInventory, EipConstant.EIP_NETWORK_SERVICE_TYPE);
            evt.setInventory(retinv);
            logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s, ip:%s]",
                    retinv.getUuid(), retinv.getName(), vipInventory.getUuid(), vipInventory.getIp()));
            bus.publish(evt);
            return;
        }

        final EipVO fevo = vo;
        EipStruct struct = new EipStruct();
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        struct.setNic(nicInventory);
        struct.setVip(vipInventory);
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);
        FlowChain chain = createEipFlowBuilder.build();
        chain.setName(String.format("create-eip-vmNic-%s-vip-%s", msg.getVmNicUuid(), msg.getVipUuid()));
        chain.getData().put(EipConstant.Params.EIP_STRUCT.toString(), struct);
        chain.getData().put(EipConstant.Params.NEED_LOCK_VIP.toString(), true);
        chain.getData().put(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString(), providerType.toString());
        chain.getData().put(VipConstant.Params.VIP.toString(), vipInventory);
        chain.getData().put(VipConstant.Params.VIP_SERVICE_PROVIDER_TYPE.toString(), providerType.toString());
        chain.getData().put(VipConstant.Params.GUEST_L3NETWORK_VIP_FOR.toString(), l3NetworkInventory);
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setInventory(retinv);
                logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s] for vm nic[uuid:%s]",
                        retinv.getUuid(), retinv.getName(), vipInventory.getUuid(), nicInventory.getUuid()));
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setErrorCode(errCode);
                dbf.remove(fevo);
                logger.debug(String.format("failed to create eip[uuid:%s, name:%s] on vip[uuid:%s] for vm nic[uuid:%s], %s",
                        retinv.getUuid(), retinv.getName(), vipInventory.getUuid(), nicInventory.getUuid(), errCode));
                bus.publish(evt);
            }
        }).start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(EipConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (EipBackend ext : pluginRgty.getExtensionList(EipBackend.class)) {
            EipBackend old = backends.get(ext.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate EipBackend[%s,%s] for type[%s]", old.getClass().getName(),
                        ext.getClass().getName(), ext.getNetworkServiceProviderType()));
            }
            backends.put(ext.getNetworkServiceProviderType(), ext);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        createEipFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(createEipFlowNames).construct();
        removeEipFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(removeEipFlowNames).construct();
        attachEipFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(attachEipFlowNames).construct();
        detachEipFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(detachEipFlowNames).construct();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public EipBackend getEipBackend(String providerType) {
        EipBackend bkd = backends.get(providerType);
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find EipBackend for type[%s]", providerType));
        }

        return bkd;
    }


    private void detachEip(final EipStruct struct, final String providerType, final boolean updateDb, final Completion completion) {
        VipInventory vip = struct.getVip();
        VmNicInventory nic = struct.getNic();
        final EipInventory eip = struct.getEip();

        FlowChain chain = detachEipFlowBuilder.build();

        chain.setProcessors(CollectionUtils.transformToList(pluginRgty.getExtensionList(DetachEipFlowChainExtensionPoint.class), new Function<FlowChainProcessor, DetachEipFlowChainExtensionPoint>() {
            @Override
            public FlowChainProcessor call(DetachEipFlowChainExtensionPoint ext) {
                return ext.createDetachEipFlowChainProcessor(struct, providerType);
            }
        }));

        chain.setName(String.format("detach-eip-%s-vmNic-%s", eip.getUuid(), nic.getUuid()));
        chain.getData().put(EipConstant.Params.EIP_STRUCT.toString(), struct);
        chain.getData().put(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString(), providerType);
        chain.getData().put(VipConstant.Params.VIP.toString(), vip);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                if (updateDb) {
                    EipVO eipvo = dbf.findByUuid(eip.getUuid(), EipVO.class);
                    eipvo.setVmNicUuid(null);
                    eipvo.setGuestIp(null);
                    dbf.update(eipvo);
                }

                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void detachEip(EipStruct struct, String providerType, final Completion completion) {
        detachEip(struct, providerType, false, completion);
    }

    @Override
    public void detachEipAndUpdateDb(EipStruct struct, String providerType, Completion completion) {
        detachEip(struct, providerType, true, completion);
    }

    @Override
    public void attachEip(final EipStruct struct, final String providerType, final Completion completion) {
        final EipInventory eip = struct.getEip();
        final VmNicInventory nic = struct.getNic();
        VipInventory vip = struct.getVip();

        L3NetworkVO l3vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        L3NetworkInventory l3inv = L3NetworkInventory.valueOf(l3vo);

        FlowChain chain = attachEipFlowBuilder.build();

        chain.setProcessors(CollectionUtils.transformToList(pluginRgty.getExtensionList(AttachEipFlowChainExtensionPoint.class), new Function<FlowChainProcessor, AttachEipFlowChainExtensionPoint>() {
            @Override
            public FlowChainProcessor call(AttachEipFlowChainExtensionPoint ext) {
                return ext.createAttachEipFlowChainProcessor(struct, providerType);
            }
        }));

        chain.setName(String.format("attach-eip-%s-vmNic-%s", eip.getUuid(), nic.getUuid()));
        chain.getData().put(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString(), providerType);
        chain.getData().put(EipConstant.Params.EIP_STRUCT.toString(), struct);
        chain.getData().put(VipConstant.Params.VIP.toString(), vip);
        chain.getData().put(VipConstant.Params.VIP_SERVICE_PROVIDER_TYPE.toString(), providerType);
        chain.getData().put(VipConstant.Params.GUEST_L3NETWORK_VIP_FOR.toString(), l3inv);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    public void setCreateEipFlowNames(List<String> createEipFlowNames) {
        this.createEipFlowNames = createEipFlowNames;
    }

    public void setRemoveEipFlowNames(List<String> removeEipFlowNames) {
        this.removeEipFlowNames = removeEipFlowNames;
    }

    public void setAttachEipFlowNames(List<String> attachEipFlowNames) {
        this.attachEipFlowNames = attachEipFlowNames;
    }

    public void setDetachEipFlowNames(List<String> detachEipFlowNames) {
        this.detachEipFlowNames = detachEipFlowNames;
    }

    @Override
    public String getVipUse() {
        return EipConstant.EIP_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void releaseServicesOnVip(VipInventory vip, final Completion completion) {
        SimpleQuery<EipVO> eq = dbf.createQuery(EipVO.class);
        eq.add(EipVO_.vipUuid, SimpleQuery.Op.EQ, vip.getUuid());
        final EipVO vo = eq.find();
        if (vo.getVmNicUuid() == null) {
            dbf.remove(vo);
            completion.success();
            return;
        }

        VmNicVO nicvo = dbf.findByUuid(vo.getVmNicUuid(), VmNicVO.class);
        VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, nicvo.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (VmInstanceState.Stopped == state) {
            dbf.remove(vo);
            completion.success();
            return;
        }

        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);
        EipStruct struct = new EipStruct();
        struct.setVip(vip);
        struct.setNic(nicInventory);
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.then(new DetachEipFlow());
        chain.setName(String.format("detach-eip-%s-vmNic-%s-for-vip-deletion", vo.getUuid(), nicvo.getUuid()));
        chain.getData().put(EipConstant.Params.EIP_STRUCT.toString(), struct);
        chain.getData().put(EipConstant.Params.NETWORK_SERVICE_PROVIDER_TYPE.toString(), providerType.toString());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                dbf.remove(vo);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<ExpandedQueryStruct>();

        ExpandedQueryStruct struct = new ExpandedQueryStruct();
        struct.setInventoryClassToExpand(VmNicInventory.class);
        struct.setExpandedField("eip");
        struct.setInventoryClass(EipInventory.class);
        struct.setForeignKey("uuid");
        struct.setExpandedInventoryKey("vmNicUuid");
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setInventoryClassToExpand(VipInventory.class);
        struct.setExpandedField("eip");
        struct.setInventoryClass(EipInventory.class);
        struct.setForeignKey("uuid");
        struct.setExpandedInventoryKey("vipUuid");
        structs.add(struct);

        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        return null;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (msg instanceof APICreateEipMsg) {
                    check((APICreateEipMsg)msg, pairs);
                }
            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(EipConstant.QUOTA_EIP_NUM);
                usage.setUsed(getUsedEip(accountUuid));
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedEip(String accountUuid) {
                String sql = "select count(eip) from EipVO eip, AccountResourceRefVO ref where ref.resourceUuid = eip.uuid and " +
                        "ref.accountUuid = :auuid and ref.resourceType = :rtype";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", EipVO.class.getSimpleName());
                Long en = q.getSingleResult();
                en = en == null ? 0 : en;
                return en;
            }

            @Transactional(readOnly = true)
            private void check(APICreateEipMsg msg, Map<String, QuotaPair> pairs) {
                long eipNum = pairs.get(EipConstant.QUOTA_EIP_NUM).getValue();
                long en = getUsedEip(msg.getSession().getAccountUuid());

                if (en + 1 > eipNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding.  The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), EipConstant.QUOTA_EIP_NUM, eipNum)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateEipMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(EipConstant.QUOTA_EIP_NUM);
        p.setValue(20);
        quota.addPair(p);

        return list(quota);
    }

    @Override
    public void vmPreAttachL3Network(final VmInstanceInventory vm, final L3NetworkInventory l3) {
        final List<String> nicUuids = CollectionUtils.transformToList(vm.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getUuid();
            }
        });

        if (nicUuids.isEmpty()) {
            return;
        }

        new Runnable() {
            @Override
            @Transactional(readOnly = true)
            public void run() {
                String sql = "select count(*) from EipVO eip, VipVO vip where eip.vipUuid = vip.uuid and vip.l3NetworkUuid = :l3Uuid" +
                        " and eip.vmNicUuid in (:nicUuids)";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("l3Uuid", l3.getUuid());
                q.setParameter("nicUuids", nicUuids);
                Long count = q.getSingleResult();
                if (count > 0) {
                    throw new OperationFailureException(errf.stringToOperationError(
                            String.format("unable to attach the L3 network[uuid:%s, name:%s] to the vm[uuid:%s, name:%s], because the L3" +
                                    " network is providing EIP to one of the vm's nic", l3.getUuid(), l3.getName(), vm.getUuid(), vm.getName())
                    ));
                }
            }
        }.run();
    }

    @Override
    public void vmIpChanged(VmInstanceInventory vm, VmNicInventory nic, UsedIpInventory oldIp, UsedIpInventory newIp) {
        SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
        q.add(EipVO_.vmNicUuid, Op.EQ, nic.getUuid());
        EipVO eip = q.find();

        if (eip == null) {
            return;
        }

        eip.setGuestIp(newIp.getIp());
        dbf.update(eip);

        logger.debug(String.format("update the EIP[uuid:%s, name:%s]'s guest IP from %s to %s for the nic[uuid:%s]",
                eip.getUuid(), eip.getName(), oldIp.getIp(), newIp.getIp(), nic.getUuid()));
    }
}
