package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginDSL;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.inventory.InventoryFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.network.l2.L2NetworkExtensionPointEmitter;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.network.l2.vxlan.vtep.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by weiwang on 01/03/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VxlanNetworkPool extends L2NoVlanNetwork implements L2VxlanNetworkPoolManager {
    private static final CLogger logger = Utils.getLogger(VxlanNetworkPool.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected L2NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L2NetworkManager l2Mgr;
    @Autowired
    protected InventoryFacade inventoryMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    private Map<String, VniAllocatorStrategy> vniAllocatorStrategies = Collections.synchronizedMap(new HashMap<String, VniAllocatorStrategy>());

    public VxlanNetworkPool(L2NetworkVO vo) {
        super(vo);
    }

    public VxlanNetworkPool(){
        super(null);
    }

    private VxlanNetworkPoolVO getSelf() {
        return (VxlanNetworkPoolVO) self;
    }

    protected L2NetworkInventory getSelfInventory() {
        return L2VxlanNetworkPoolInventory.valueOf(getSelf());
    }

    @Override
    public void deleteHook() {
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof AllocateVniMsg) {
            handle((AllocateVniMsg) msg);
        } else if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CreateVtepMsg) {
            handle((CreateVtepMsg) msg);
        } else if (msg instanceof DeleteVtepMsg) {
            handle((DeleteVtepMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {
        final PrepareL2NetworkOnHostReply reply = new PrepareL2NetworkOnHostReply();
        prepareL2NetworkOnHosts(msg.getL2NetworkUuid(), Arrays.asList(msg.getHost()), new Completion(msg) {
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

    private void handle(AllocateVniMsg msg) {
        VniAllocatorType strategyType = msg.getAllocateStrategy() == null ? RandomVniAllocatorStrategy.type : VniAllocatorType.valueOf(msg.getAllocateStrategy());
        VniAllocatorStrategy vas = getVniAllocatorStrategy(strategyType);
        AllocateVniReply reply = new AllocateVniReply();
        Integer vni = vas.allocateVni(msg);
        if (vni == null) {
            reply.setError(errf.instantiateErrorCode(L2Errors.ALLOCATE_VNI_ERROR, String.format("Vni allocator strategy[%s] returns nothing, because no vni is available in this VxlanNetwork[name:%s, uuid:%s]", strategyType, self.getName(), self.getUuid())));
        } else {
            logger.debug(String.format("Vni allocator strategy[%s] successfully allocates an vni[%s]", strategyType, vni));
            reply.setVni(vni);
        }

        bus.reply(msg, reply);
    }

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkInventory inv = L2NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        dbf.removeByPrimaryKey(msg.getL2NetworkUuid(), L2NetworkVO.class);
        extpEmitter.afterDelete(inv);

        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();
        bus.reply(msg, reply);
    }

    private void handle(CreateVtepMsg msg) {
        List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, msg.getPoolUuid()).eq(VtepVO_.vtepIp, msg.getVtepIp()).list();
        for (VtepVO vtep: vteps) {
            if (!vtep.getHostUuid().equals(msg.getHostUuid())) {
                logger.warn(String.format("same vtepip[%s] in host[%s] and host[%s], which in same cluster[%s]",
                        msg.getVtepIp(), vtep.getHostUuid(), msg.getHostUuid(), msg.getClusterUuid()));
            } else {
                logger.debug(String.format("get duplicate vtep create msg for ip [%s] in host [%s]",
                        msg.getVtepIp(), vtep.getHostUuid()));
            }

            CreateVtepReply reply = new CreateVtepReply();
            reply.setInventory(VtepInventory.valueOf(vtep));
            bus.reply(msg, reply);

            return;
        }

        VtepVO vo = new VtepVO();

        vo.setUuid(Platform.getUuid());
        vo.setClusterUuid(msg.getClusterUuid());
        vo.setHostUuid(msg.getHostUuid());
        vo.setPort(msg.getPort());
        vo.setType(msg.getType());
        vo.setVtepIp(msg.getVtepIp());
        vo.setPoolUuid(msg.getPoolUuid());
        try {
            vo = dbf.persistAndRefresh(vo);
        } catch (Throwable t) {
            if (!ExceptionDSL.isCausedBy(t, ConstraintViolationException.class)) {
                throw t;
            }

            // the vtep is already attached
        }

        VtepInventory inv = VtepInventory.valueOf(vo);
        String info = String.format("successfully create Vtep, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);

        CreateVtepReply reply = new CreateVtepReply();
        reply.setInventory(inv);
        bus.reply(msg, reply);
    }

    private void handle(DeleteVtepMsg msg) {
        DeleteVtepReply reply = new DeleteVtepReply();
        VtepVO vo = dbf.findByUuid(msg.getVtepUuid(), VtepVO.class);
        dbf.remove(vo);
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            handle((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APICreateVniRangeMsg) {
            handle((APICreateVniRangeMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIDeleteVniRangeMsg) {
            handle((APIDeleteVniRangeMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIDetachL2NetworkFromClusterMsg msg) {
        final APIDetachL2NetworkFromClusterEvent evt = new APIDetachL2NetworkFromClusterEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("detach-l2-vxlan-pool-%s", msg.getL2NetworkUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> uuids = Q.New(VxlanNetworkVO.class)
                        .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid()).listValues();

                if (uuids.isEmpty()) {
                    logger.info(String.format("There are no vxlan networks for vxlan pool %s", msg.getL2NetworkUuid()));
                    trigger.next();
                    return;
                }

                logger.info(String.format("Detach l2 vxlan networks %s for vxlan pool %s", uuids, msg.getL2NetworkUuid()));

                new While<>(uuids).all((uuid, completion) -> {
                    L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                    dmsg.setL2NetworkUuid(uuid);
                    dmsg.setClusterUuid(msg.getClusterUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                    bus.send(dmsg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
                            } else {
                                logger.debug(String.format("Detach l2 vxlan network %s for vxlan pool %s success", uuid, msg.getL2NetworkUuid()));
                            }
                            completion.done();
                        }
                    });
                }).run(new NoErrorCompletion(msg) {
                    @Override
                    public void done() {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                L2NetworkDetachFromClusterMsg dmsg = new L2NetworkDetachFromClusterMsg();
                dmsg.setL2NetworkUuid(msg.getL2NetworkUuid());
                dmsg.setClusterUuid(msg.getClusterUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                bus.send(dmsg, new CloudBusCallBack(dmsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String tag = TagUtils.tagPatternToSqlPattern(VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.instantiateTag(map(
                        e(VxlanSystemTags.VXLAN_POOL_UUID_TOKEN, msg.getL2NetworkUuid()),
                        e(VxlanSystemTags.CLUSTER_UUID_TOKEN, msg.getClusterUuid()))
                ));

                VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.delete(msg.getL2NetworkUuid(), tag);

                trigger.next();
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(final APICreateVniRangeMsg msg) {
        VniRangeVO vo = new VniRangeVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setStartVni(msg.getStartVni());
        vo.setEndVni(msg.getEndVni());
        vo.setL2NetworkUuid(msg.getL2NetworkUuid());
        vo = dbf.persistAndRefresh(vo);
        VniRangeInventory inv = VniRangeInventory.valueOf(vo);
        String info = String.format("successfully create VniRange, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);

        APICreateVniRangeEvent evt = new APICreateVniRangeEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
        final APIAttachL2NetworkToClusterEvent evt = new APIAttachL2NetworkToClusterEvent(msg.getId());
        SimpleQuery<L2NetworkClusterRefVO> rq = dbf.createQuery(L2NetworkClusterRefVO.class);
        rq.add(L2NetworkClusterRefVO_.clusterUuid, SimpleQuery.Op.EQ, msg.getClusterUuid());
        rq.add(L2NetworkClusterRefVO_.l2NetworkUuid, SimpleQuery.Op.EQ, msg.getL2NetworkUuid());
        long count = rq.count();
        if (count != 0) {
            evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
            bus.publish(evt);
            return;
        }

        for (String tag : msg.getSystemTags()) {
            tagMgr.createNonInherentSystemTag(msg.getL2NetworkUuid(), tag, L2NetworkVO.class.getSimpleName());
        }

        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.add(HostVO_.clusterUuid, SimpleQuery.Op.EQ, msg.getClusterUuid());
        query.add(HostVO_.state, SimpleQuery.Op.NOT_IN, HostState.PreMaintenance, HostState.Maintenance);
        query.add(HostVO_.status, SimpleQuery.Op.EQ, HostStatus.Connected);
        final List<HostVO> hosts = query.list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);

        prepareL2NetworkOnHosts(msg.getL2NetworkUuid(), hvinvs, new Completion(msg) {
            @Override
            public void success() {
                L2NetworkClusterRefVO rvo = new L2NetworkClusterRefVO();
                rvo.setClusterUuid(msg.getClusterUuid());
                rvo.setL2NetworkUuid(self.getUuid());
                dbf.persist(rvo);

                List<String> uuids = Q.New(VxlanNetworkVO.class)
                        .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid()).listValues();
                for (String uuid : uuids) {
                    rvo = new L2NetworkClusterRefVO();
                    rvo.setClusterUuid(msg.getClusterUuid());
                    rvo.setL2NetworkUuid(uuid);
                    dbf.persist(rvo);
                }


                logger.debug(String.format("successfully attached L2VxlanNetworkPool[uuid:%s] to cluster [uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                for (String tag : msg.getSystemTags()) {
                    VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.delete(msg.getL2NetworkUuid(), tag);
                }
                evt.setError(errf.instantiateErrorCode(L2Errors.ATTACH_ERROR, errorCode));
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDeleteL2NetworkMsg msg) {
        List<String> uuids = Q.New(VxlanNetworkVO.class)
                .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, msg.getL2NetworkUuid()).listValues();
        ErrorCodeList errList = new ErrorCodeList();

        new While<>(uuids).all((String uuid, WhileCompletion completion) -> {
            DeleteL2NetworkMsg dmsg = new DeleteL2NetworkMsg();
            dmsg.setUuid(uuid);
            dmsg.setForceDelete(msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Enforcing ? true : false);
            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
            bus.send(dmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    }
                    completion.done();
                }
            });
        }).run(new NoErrorCompletion() {
            @Override
            public void done() {
                if (!errList.getCauses().isEmpty()) {
                    APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
                    bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
                    evt.setError(errList.getCauses().get(0));
                    bus.publish(evt);
                } else {
                    superHandle((L2NetworkMessage) msg);
                }
            }
        });
    }

    private void handle(final APIDeleteVniRangeMsg msg) {
        APIDeleteVniRangeEvent evt = new APIDeleteVniRangeEvent(msg.getId());
        VniRangeVO vo = Q.New(VniRangeVO.class).eq(VniRangeVO_.uuid, msg.getUuid()).find();

        List<String> uuids = Q.New(VxlanNetworkVO.class)
                .select(VxlanNetworkVO_.uuid).eq(VxlanNetworkVO_.poolUuid, vo.getL2NetworkUuid())
                .gte(VxlanNetworkVO_.vni, vo.getStartVni()).lte(VxlanNetworkVO_.vni, vo.getEndVni()).listValues();

        if (uuids.isEmpty()) {
            logger.info(String.format("there are no vxlan networks for vni range[%s] and delete vni range directly", msg.getUuid()));
            dbf.remove(vo);
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
            bus.publish(evt);
        }

        logger.info(String.format("delete l2 vxlan networks[%s] for vni range[%s]", uuids, msg.getUuid()));

        new While<>(uuids).all((uuid, completion) -> {
            DeleteL2NetworkMsg dmsg = new DeleteL2NetworkMsg();
            dmsg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
            bus.send(dmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    } else {
                        logger.debug(String.format("delete l2 vxlan network %s for vni range %s success", uuid, msg.getL2NetworkUuid()));
                    }
                    completion.done();
                }
            });
        }).run(new NoErrorCompletion(msg) {
            @Override
            public void done() {
                dbf.remove(vo);
                bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, msg.getL2NetworkUuid());
                bus.publish(evt);
            }
        });
    }

    private void prepareL2NetworkOnHosts(final String l2NetworkUuid, final List<HostInventory> hosts, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-%s-on-hosts", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                new While<>(hosts).all((h, completion1) -> {
                    CheckL2NetworkOnHostMsg cmsg = new CheckL2NetworkOnHostMsg();
                    cmsg.setHostUuid(h.getUuid());
                    cmsg.setL2NetworkUuid(l2NetworkUuid);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2NetworkUuid);
                    bus.send(cmsg, new CloudBusCallBack(completion1) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                trigger.fail(reply.getError());
                            }
                            completion1.done();
                        }
                    });
                }).run(new NoErrorCompletion() {
                    @Override
                    public void done() {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            private void realize(final Iterator<HostInventory> it, final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    trigger.next();
                    return;
                }

                HostInventory host = it.next();
                realizeNetwork(host.getUuid(), host.getHypervisorType(), new Completion(trigger) {
                    @Override
                    public void success() {
                        realize(it, trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                realize(hosts.iterator(), trigger);
            }
        }).done(new FlowDoneHandler(completion) {
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

    protected void realizeNetwork(String hostUuid, String htype, Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType);
        ext.realize(getSelfInventory(), hostUuid, completion);
    }

    private void superHandle(L2NetworkMessage msg) {
        super.handleMessage((Message) msg);
    }

    @Override
    public VniAllocatorStrategy getVniAllocatorStrategy(VniAllocatorType type) {
        for (VniAllocatorStrategy f : pluginRgty.getExtensionList(VniAllocatorStrategy.class)) {
            VniAllocatorStrategy old = vniAllocatorStrategies.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VniAllocatorStrategy[%s, %s] for type[%s]", f.getClass().getName(),
                        old.getClass().getName(), f.getType()));
            }
            vniAllocatorStrategies.put(f.getType().toString(), f);
        }

        VniAllocatorStrategy factory = vniAllocatorStrategies.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find VniAllocatorStrategy for type(%s)", type));
        }

        return factory;
    }

    public Map<String, String> getAttachedCidrs(String l2NetworkUuid) {
        List<Map<String, String>> tokenList = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensOfTagsByResourceUuid(l2NetworkUuid);

        Map<String, String> attachedClusters = new HashMap<>();
        for (Map<String, String> tokens : tokenList) {
            attachedClusters.put(tokens.get(VxlanSystemTags.CLUSTER_UUID_TOKEN),
                    tokens.get(VxlanSystemTags.VTEP_CIDR_TOKEN).split("[{}]")[1]);
        }
        return attachedClusters;
    }

}
