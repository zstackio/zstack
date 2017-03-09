package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.inventory.InventoryFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NetworkExtensionPointEmitter;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.L2NoVlanNetwork;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * Created by weiwang on 01/03/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VxlanNetworkPool extends L2NoVlanNetwork {
    private static final CLogger logger = Utils.getLogger(VxlanNetworkPool.class);

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

    protected VxlanNetworkPoolVO self;

    public VxlanNetworkPool(L2NetworkVO self) {
        super(self);
    }

    private VxlanNetworkPoolVO getSelf() {
        return (VxlanNetworkPoolVO) self;
    }

    @Override
    public void deleteHook() {
    }

    protected L2VxlanNetworkPoolInventory getSelfInventory() {
        return L2VxlanNetworkPoolInventory.valueOf(self);
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
        if (msg instanceof L2NetworkDeletionMsg) {
            handle((L2NetworkDeletionMsg) msg);
        } else if (msg instanceof CheckL2NetworkOnHostMsg) {
            handle((CheckL2NetworkOnHostMsg) msg);
        } else if (msg instanceof PrepareL2NetworkOnHostMsg) {
            handle((PrepareL2NetworkOnHostMsg) msg);
        } else if (msg instanceof DetachL2NetworkFromClusterMsg) {
            handle((DetachL2NetworkFromClusterMsg) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DetachL2NetworkFromClusterMsg msg) {

    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {

    }

    private void handle(final CheckL2NetworkOnHostMsg msg) {

    }

    private void handle(L2NetworkDeletionMsg msg) {

    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            handle((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APICreateVniRangeMsg) {
            handle((APICreateVniRangeMsg) msg);
        } else if (msg instanceof L2NetworkMessage) {
            superHandle((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APICreateVniRangeMsg msg) {
        VniRangeVO vo = new VniRangeVO();
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

        tagMgr.createTagsFromAPICreateMessage(msg, msg.getL2NetworkUuid(), VxlanNetworkPoolVO.class.getSimpleName());
        List<Map<String, String>> tokenList = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensOfTagsByResourceUuid(msg.getL2NetworkUuid());
        Map<String, String> attachedClusters = new HashMap<>();
        for (Map<String, String> tokens : tokenList) {
            attachedClusters.put(tokens.get(VxlanSystemTags.CLUSTER_UUID_TOKEN),
                    tokens.get(VxlanSystemTags.VTEP_CIDR_TOKEN));
        }
        self.setAttachedCidrs(attachedClusters);

        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.add(HostVO_.clusterUuid, SimpleQuery.Op.EQ, msg.getClusterUuid());
        query.add(HostVO_.state, SimpleQuery.Op.NOT_IN, HostState.PreMaintenance, HostState.Maintenance);
        query.add(HostVO_.status, SimpleQuery.Op.EQ, HostStatus.Connected);
        final List<HostVO> hosts = query.list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);

        prepareL2NetworkOnHosts(self.getAttachedCidrs().get(msg.getClusterUuid()), hvinvs, new Completion(msg) {
            @Override
            public void success() {
                L2NetworkClusterRefVO rvo = new L2NetworkClusterRefVO();
                rvo.setClusterUuid(msg.getClusterUuid());
                rvo.setL2NetworkUuid(self.getUuid());
                dbf.persist(rvo);
                logger.debug(String.format("successfully attached L2VxlanNetworkPool[uuid:%s] to cluster [uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.findByUuid(self.getUuid(), VxlanNetworkPoolVO.class);
                evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errf.instantiateErrorCode(L2Errors.ATTACH_ERROR, errorCode));
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDeleteL2NetworkMsg msg) {
    }

    private void prepareL2NetworkOnHosts(final String cidr, final List<HostInventory> hosts, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-%s-on-hosts", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<CheckNetworkHostCidrMsg> cmsgs = new ArrayList<>();
                for (HostInventory h : hosts) {
                    CheckNetworkHostCidrMsg cmsg = new CheckNetworkHostCidrMsg();
                    cmsg.setHostUuid(h.getUuid());
                    cmsg.setCidr(self.getAttachedCidrs().get(cidr));
                    bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, h.getUuid());
                    cmsgs.add(cmsg);
                }

                if (cmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.send(cmsgs, new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply r : replies) {
                            if (!r.isSuccess()) {
                                trigger.fail(r.getError());
                                return;
                            }
                        }

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

    private void superHandle(L2NetworkMessage msg) {
        super.handleMessage((Message) msg);
    }
}
