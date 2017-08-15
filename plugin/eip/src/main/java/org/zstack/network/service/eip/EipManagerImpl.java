package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.quota.QuotaConstant;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class EipManagerImpl extends AbstractService implements EipManager, VipReleaseExtensionPoint,
        AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, VmPreAttachL3NetworkExtensionPoint,
        VmIpChangedExtensionPoint, ResourceOwnerAfterChangeExtensionPoint {
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

    private Map<String, EipBackend> backends = new HashMap<>();

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
        if (msg instanceof EipDeletionMsg) {
            handle((EipDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(EipDeletionMsg msg) {
        EipDeletionReply reply = new EipDeletionReply();
        deleteEip(msg.getEipUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
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
    private List<VmNicInventory> getAttachableVmNicForEip(VipInventory vip) {
        String providerType = vip.getServiceProvider();
        String peerL3NetworkUuid = vip.getPeerL3NetworkUuid();
        String zoneUuid = Q.New(L3NetworkVO.class)
                .select(L3NetworkVO_.zoneUuid)
                .eq(L3NetworkVO_.uuid, vip.getL3NetworkUuid())
                .findValue();

        List<String> l3Uuids;

        if (providerType != null) {
            // the eip is created on the backend
            l3Uuids = SQL.New("select l3.uuid" +
                    " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO np" +
                    " where l3.system = :system" +
                    " and l3.uuid != :vipL3NetworkUuid" +
                    " and l3.uuid = ref.l3NetworkUuid" +
                    " and ref.networkServiceType = :nsType" +
                    " and l3.zoneUuid = :zoneUuid" +
                    " and np.uuid = ref.networkServiceProviderUuid" +
                    " and np.type = :npType")
                    .param("system", false)
                    .param("zoneUuid", zoneUuid)
                    .param("nsType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                    .param("npType", providerType)
                    .param("vipL3NetworkUuid", vip.getL3NetworkUuid())
                    .list();
        } else {
            // the eip is not created on the backend
            l3Uuids = SQL.New("select l3.uuid" +
                    " from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                    " where l3.system = :system" +
                    " and l3.uuid != :vipL3NetworkUuid" +
                    " and l3.uuid = ref.l3NetworkUuid" +
                    " and ref.networkServiceType = :nsType" +
                    " and l3.zoneUuid = :zoneUuid")
                    .param("system", false)
                    .param("zoneUuid", zoneUuid)
                    .param("nsType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                    .param("vipL3NetworkUuid", vip.getL3NetworkUuid())
                    .list();
        }

        if (peerL3NetworkUuid != null) {
            l3Uuids = l3Uuids.stream().filter(l -> l.equals(peerL3NetworkUuid)).collect(Collectors.toList());
        }

        if (l3Uuids.isEmpty()) {
            return new ArrayList<>();
        }

        List<VmNicVO> nics  = SQL.New("select nic" +
                " from VmNicVO nic, VmInstanceVO vm" +
                " where nic.l3NetworkUuid in (:l3Uuids)" +
                " and nic.vmInstanceUuid = vm.uuid" +
                " and vm.type = :vmType and vm.state in (:vmStates) " +
                // IP = null means the VM is just recovered without any IP allocated
                " and nic.ip is not null")
                .param("l3Uuids", l3Uuids)
                .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                .param("vmStates", EipConstant.attachableVmStates)
                .list();
        return VmNicInventory.valueOf(nics);
    }

    private List<VmNicInventory> filterVmNicsForEipInVirtualRouterExtensionPoint(VipInventory vip, List<VmNicInventory> vmNics) {
        if (vmNics.isEmpty()){
            return vmNics;
        }

        List<VmNicInventory> ret = new ArrayList<>(vmNics);
        for (FilterVmNicsForEipInVirtualRouterExtensionPoint extp : pluginRgty.getExtensionList(FilterVmNicsForEipInVirtualRouterExtensionPoint.class)) {
            ret = extp.filterVmNicsForEipInVirtualRouter(vip, ret);
        }
        return ret;
    }

    @Transactional(readOnly = true)
    private List<VmNicInventory> getEipAttachableVmNics(APIGetEipAttachableVmNicsMsg msg){
        VipVO vipvo = msg.getEipUuid() == null ?
                Q.New(VipVO.class).eq(VipVO_.uuid, msg.getVipUuid()).find() :
                SQL.New("select vip" +
                        " from VipVO vip, EipVO eip" +
                        " where eip.uuid = :eipUuid" +
                        " and eip.vipUuid = vip.uuid")
                        .param("eipUuid", msg.getEipUuid())
                        .find();
        VipInventory vipInv = VipInventory.valueOf(vipvo);
        List<VmNicInventory> nics = getAttachableVmNicForEip(vipInv);
        return filterVmNicsForEipInVirtualRouterExtensionPoint(vipInv, nics);
    }

    private void handle(APIGetEipAttachableVmNicsMsg msg) {
        APIGetEipAttachableVmNicsReply reply = new APIGetEipAttachableVmNicsReply();
        boolean isAttached = Q.New(EipVO.class).eq(EipVO_.uuid, msg.getEipUuid()).notNull(EipVO_.vmNicUuid).isExists();
        reply.setInventories(isAttached ? new ArrayList<>() : getEipAttachableVmNics(msg));
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

        NetworkServiceProviderType providerType = nwServiceMgr.
                getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);
        EipStruct struct = new EipStruct();
        struct.setVip(vipInventory);
        struct.setNic(nicInventory);
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        detachEipAndUpdateDb(struct, providerType.toString(), new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(EipInventory.valueOf(dbf.reload(vo)));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
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
        if (EipConstant.noNeedApplyOnBackendVmStates.contains(state)) {
            vo.setVmNicUuid(nicInventory.getUuid());
            vo.setGuestIp(nicvo.getIp());
            EipVO evo = dbf.updateAndRefresh(vo);
            evt.setInventory(EipInventory.valueOf(evo));
            bus.publish(evt);
            return;
        }

        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);
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
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void deleteEip(String eipUuid, Completion completion) {
        final EipVO vo = dbf.findByUuid(eipUuid, EipVO.class);
        VipVO vipvo = dbf.findByUuid(vo.getVipUuid(), VipVO.class);
        VipInventory vipInventory = VipInventory.valueOf(vipvo);

        if (vo.getVmNicUuid() == null) {
            new Vip(vipvo.getUuid()).release(new Completion(completion) {
                @Override
                public void success() {
                    dbf.remove(vo);
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });

            return;
        }

        VmNicVO nicvo = dbf.findByUuid(vo.getVmNicUuid(), VmNicVO.class);
        VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);

        EipStruct struct = new EipStruct();
        struct.setNic(nicInventory);
        struct.setVip(vipInventory);
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        NetworkServiceProviderType providerType = nwServiceMgr.
                getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-eip-vmNic-%s-vip-%s", nicvo.getUuid(), vipvo.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-eip-from-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType.toString());
                        bkd.revokeEip(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: add GC instead of failing the API
                                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "release-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Vip(vipInventory.getUuid()).release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        dbf.remove(vo);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(APIDeleteEipMsg msg) {
        final APIDeleteEipEvent evt = new APIDeleteEipEvent(msg.getId());

        deleteEip(msg.getEipUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });

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
        EipVO finalVo1 = vo;
        vo = new SQLBatchWithReturn<EipVO>() {
            @Override
            protected EipVO scripts() {
                persist(finalVo1);
                reload(finalVo1);
                acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), finalVo1.getUuid(), EipVO.class);
                tagMgr.createTagsFromAPICreateMessage(msg, finalVo1.getUuid(), EipVO.class.getSimpleName());
                return finalVo1;
            }
        }.execute();

        VipVO vipvo = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        final VipInventory vipInventory = VipInventory.valueOf(vipvo);

        if (vo.getVmNicUuid() == null) {
            EipVO finalVo = vo;
            Vip vip = new Vip(vipvo.getUuid());
            vip.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
            vip.acquire(false, new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(EipInventory.valueOf(finalVo));
                    logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s, ip:%s]",
                            finalVo.getUuid(), finalVo.getName(), vipInventory.getUuid(), vipInventory.getIp()));
                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });

            return;
        }

        VmNicVO nicvo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        vo.setGuestIp(nicvo.getIp());
        vo = dbf.updateAndRefresh(vo);
        final EipInventory retinv = EipInventory.valueOf(vo);

        final VmNicInventory nicInventory = VmNicInventory.valueOf(nicvo);

        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, nicvo.getVmInstanceUuid());
        VmInstanceState state = q.findValue();

        if (state != VmInstanceState.Running) {
            EipVO finalVo = vo;
            new Vip(vipvo.getUuid()).acquire(false, new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(EipInventory.valueOf(finalVo));
                    logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s, ip:%s]",
                            finalVo.getUuid(), finalVo.getName(), vipInventory.getUuid(), vipInventory.getIp()));
                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });

            return;
        }

        final EipVO fevo = vo;
        EipStruct struct = new EipStruct();
        struct.setEip(EipInventory.valueOf(vo));
        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        struct.setNic(nicInventory);
        struct.setVip(vipInventory);
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nicInventory.getL3NetworkUuid(), EipConstant.EIP_TYPE);
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-eip-vmNic-%s-vip-%s", msg.getVmNicUuid(), msg.getVipUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "prepare-vip";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Vip vip = new Vip(vipInventory.getUuid());
                        vip.setServiceProvider(providerType.toString());
                        vip.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        vip.setPeerL3NetworkUuid(nicInventory.getL3NetworkUuid());
                        vip.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!s) {
                            trigger.rollback();
                            return;
                        }

                        Vip vip = new Vip(vipInventory.getUuid());
                        vip.release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(errorCode.toString());
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-eip-on-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType.toString());
                        bkd.applyEip(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(retinv);
                        logger.debug(String.format("successfully created eip[uuid:%s, name:%s] on vip[uuid:%s] for vm nic[uuid:%s]",
                                retinv.getUuid(), retinv.getName(), vipInventory.getUuid(), nicInventory.getUuid()));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        dbf.remove(fevo);
                        logger.debug(String.format("failed to create eip[uuid:%s, name:%s] on vip[uuid:%s] for vm nic[uuid:%s], %s",
                                retinv.getUuid(), retinv.getName(), vipInventory.getUuid(), nicInventory.getUuid(), errCode));
                        bus.publish(evt);
                    }
                });
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
        VmNicInventory nic = struct.getNic();
        final EipInventory eip = struct.getEip();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("detach-eip-%s-vmNic-%s", eip.getUuid(), nic.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-eip-from-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType);
                        bkd.revokeEip(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO add GC instead of failing the API
                                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-vip-from-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Vip(eip.getVipUuid()).deleteFromBackend(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                if (updateDb) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "udpate-eip";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            UpdateQuery q = UpdateQuery.New(EipVO.class);
                            q.condAnd(EipVO_.uuid, Op.EQ, eip.getUuid());
                            q.set(EipVO_.vmNicUuid, null);
                            q.set(EipVO_.guestIp, null);
                            q.update();

                            trigger.next();
                        }
                    });
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
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


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("attach-eip-%s-vmNic-%s", eip.getUuid(), nic.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    boolean s = false;

                    String __name__ = "acquire-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Vip vip = new Vip(struct.getVip().getUuid());
                        vip.setServiceProvider(providerType);
                        vip.setUseFor(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                        vip.setPeerL3NetworkUuid(nic.getL3NetworkUuid());
                        vip.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!s) {
                            trigger.rollback();
                            return;
                        }

                        Vip vip = new Vip(struct.getVip().getUuid());
                        vip.release(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(errorCode.toString());
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-eip-on-backend";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        EipBackend bkd = getEipBackend(providerType);
                        bkd.applyEip(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
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

        EipBackend bkd = getEipBackend(providerType.toString());
        bkd.revokeEip(struct, new Completion(completion) {
            @Override
            public void success() {
                dbf.remove(vo);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to detach eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider will garbage collect. %s",
                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode));
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<>();

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
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreateEipMsg) {
                        check((APICreateEipMsg) msg, pairs);
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
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(EipConstant.QUOTA_EIP_NUM);
                usage.setUsed(getUsedEipNum(accountUuid));
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedEipNum(String accountUuid) {
                String sql = "select count(eip)" +
                        " from EipVO eip, AccountResourceRefVO ref" +
                        " where ref.resourceUuid = eip.uuid" +
                        " and ref.accountUuid = :auuid" +
                        " and ref.resourceType = :rtype";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", EipVO.class.getSimpleName());
                Long usedEipNum = q.getSingleResult();
                usedEipNum = usedEipNum == null ? 0 : usedEipNum;
                return usedEipNum;
            }

            @Transactional(readOnly = true)
            private long getVmEipNum(String vmUuid) {
                String sql = "select count(eip)" +
                        " from EipVO eip, VmNicVO vmnic" +
                        " where vmnic.vmInstanceUuid = :vmuuid" +
                        " and vmnic.uuid = eip.vmNicUuid";

                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("vmuuid", vmUuid);
                Long vmEipNum = q.getSingleResult();
                vmEipNum = vmEipNum == null ? 0 : vmEipNum;
                return vmEipNum;
            }

            @Transactional(readOnly = true)
            private void check(APICreateEipMsg msg, Map<String, QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

                long eipNumQuota = pairs.get(EipConstant.QUOTA_EIP_NUM).getValue();
                long usedEipNum = getUsedEipNum(msg.getSession().getAccountUuid());
                long askedEipNum = 1;

                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                quotaCompareInfo.quotaName = EipConstant.QUOTA_EIP_NUM;
                quotaCompareInfo.quotaValue = eipNumQuota;
                quotaCompareInfo.currentUsed = usedEipNum;
                quotaCompareInfo.request = askedEipNum;
                new QuotaUtil().CheckQuota(quotaCompareInfo);
            }

            @Transactional(readOnly = true)
            private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
                if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
                    return;
                }

                SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
                q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, msg.getResourceUuid());
                AccountResourceRefVO accResRefVO = q.find();


                if (accResRefVO.getResourceType().equals(VmInstanceVO.class.getSimpleName())) {
                    long eipNumQuota = pairs.get(EipConstant.QUOTA_EIP_NUM).getValue();
                    long usedEipNum = getUsedEipNum(resourceTargetOwnerAccountUuid);
                    long askedEipNum = getVmEipNum(msg.getResourceUuid());

                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = EipConstant.QUOTA_EIP_NUM;
                    quotaCompareInfo.quotaValue = eipNumQuota;
                    quotaCompareInfo.currentUsed = usedEipNum;
                    quotaCompareInfo.request = askedEipNum;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateEipMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(EipConstant.QUOTA_EIP_NUM);
        p.setValue(QuotaConstant.QUOTA_EIP_NUM);
        quota.addPair(p);

        return list(quota);
    }

    @Override
    public void vmPreAttachL3Network(final VmInstanceInventory vm, final L3NetworkInventory l3) {
        final List<String> nicUuids = CollectionUtils.transformToList(vm.getVmNics(),
                new Function<String, VmNicInventory>() {
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
                String sql = "select count(*)" +
                        " from EipVO eip, VipVO vip" +
                        " where eip.vipUuid = vip.uuid" +
                        " and vip.l3NetworkUuid = :l3Uuid" +
                        " and eip.vmNicUuid in (:nicUuids)";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("l3Uuid", l3.getUuid());
                q.setParameter("nicUuids", nicUuids);
                Long count = q.getSingleResult();
                if (count > 0) {
                    throw new OperationFailureException(operr("unable to attach the L3 network[uuid:%s, name:%s] to the vm[uuid:%s, name:%s]," +
                                    " because the L3 network is providing EIP to one of the vm's nic",
                            l3.getUuid(), l3.getName(), vm.getUuid(), vm.getName()));
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


    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        changeEipOwner(ref, newOwnerUuid);
    }

    @Transactional
    private void changeEipOwner(AccountResourceRefInventory ref, String newOwnerUuid) {
        String sql = "select eip.uuid" +
                " from VmInstanceVO vm, VmNicVO nic, EipVO eip" +
                " where vm.uuid = nic.vmInstanceUuid" +
                " and nic.uuid = eip.vmNicUuid" +
                " and vm.uuid = :uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuid", ref.getResourceUuid());
        List<String> eipUuids = q.getResultList();
        if (eipUuids.isEmpty()) {
            logger.debug(String.format("Vm[uuid:%s] doesn't have any eip, there is no need to change owner of eip.",
                    ref.getResourceUuid()));
            return;
        }

        for (String uuid : eipUuids) {
            acntMgr.changeResourceOwner(uuid, newOwnerUuid);
        }
    }


}
