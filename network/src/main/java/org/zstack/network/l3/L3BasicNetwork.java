package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.ResourceBindingStrategy;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.SharedResourceVO;
import org.zstack.header.identity.SharedResourceVO_;
import org.zstack.header.message.*;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.identity.AccountManager;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L3BasicNetwork implements L3Network {
    private static final CLogger logger = Utils.getLogger(L3BasicNetwork.class);
    private static final FieldPrinter printer = Utils.getFieldPrinter();

    @Autowired
    protected L3NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected L3NetworkManager l3NwMgr;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected TagManager tagMgr;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceConfigFacade rcf;

    private L3NetworkVO self;

    public L3BasicNetwork(L3NetworkVO vo) {
        this.self = vo;
    }

    protected L3NetworkVO getSelf() {
        return self;
    }

    protected L3NetworkInventory getSelfInventory() {
        return L3NetworkInventory.valueOf(getSelf());
    }

    private String getSyncId() {
        return String.format("operate-l3-%s", self.getUuid());
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

    @Override
    public void deleteHook() {
    }

    private void setIpRangeSharedResource(String L3NetworkUuid, String IpRangeUuid) {
        SharedResourceVO l3NetworkSharedResource = Q.New(SharedResourceVO.class)
                .eq(SharedResourceVO_.resourceUuid, L3NetworkUuid)
                .eq(SharedResourceVO_.resourceType, L3NetworkVO.class.getSimpleName())
                .limit(1)
                .find();
        if (l3NetworkSharedResource == null) {
            return;
        }
        SharedResourceVO svo = new SharedResourceVO();
        svo.setOwnerAccountUuid(l3NetworkSharedResource.getOwnerAccountUuid());
        svo.setResourceType(IpRangeVO.class.getSimpleName());
        svo.setResourceUuid(IpRangeUuid);
        if (l3NetworkSharedResource.isToPublic()) {
            svo.setToPublic(l3NetworkSharedResource.isToPublic());
        } else {
            svo.setReceiverAccountUuid(l3NetworkSharedResource.getReceiverAccountUuid());
        }
        dbf.persistAndRefresh(svo);
    }

    private void handle(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);

        IpRangeFactory factory = l3NwMgr.getIpRangeFactory(ipr.getIpRangeType());
        IpRangeInventory inv = factory.createIpRange(ipr, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, inv.getL3NetworkUuid(), L3NetworkVO.class.getSimpleName());

        setIpRangeSharedResource(msg.getL3NetworkUuid(), inv.getUuid());

        APIAddIpRangeEvent evt = new APIAddIpRangeEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AllocateIpMsg) {
            handle((AllocateIpMsg)msg);
        } else if (msg instanceof ReturnIpMsg) {
            handle((ReturnIpMsg)msg);
        } else if (msg instanceof L3NetworkDeletionMsg) {
            handle((L3NetworkDeletionMsg) msg);
        } else if (msg instanceof IpRangeDeletionMsg) {
            handle((IpRangeDeletionMsg) msg);
        } else if (msg instanceof CheckIpAvailabilityMsg) {
            handle((CheckIpAvailabilityMsg) msg);
        } else if (msg instanceof AttachNetworkServiceToL3Msg) {
            handle((AttachNetworkServiceToL3Msg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CheckIpAvailabilityMsg msg) {
        CheckIpAvailabilityReply reply = checkIpAvailability(msg.getIp());
        bus.reply(msg, reply);
    }

    private void handle(IpRangeDeletionMsg msg) {
        IpRangeDeletionReply reply = new IpRangeDeletionReply();

        List<IpRangeDeletionExtensionPoint> exts = pluginRgty.getExtensionList(IpRangeDeletionExtensionPoint.class);
        IpRangeVO iprvo = dbf.findByUuid(msg.getIpRangeUuid(), IpRangeVO.class);
        if (iprvo == null) {
            bus.reply(msg, reply);
            return;
        }

        final IpRangeInventory inv = IpRangeInventory.valueOf(iprvo);

        for (IpRangeDeletionExtensionPoint ext : exts) {
            ext.preDeleteIpRange(inv);
        }

        CollectionUtils.safeForEach(exts, new ForEachFunction<IpRangeDeletionExtensionPoint>() {
            @Override
            public void run(IpRangeDeletionExtensionPoint arg) {
                arg.beforeDeleteIpRange(inv);
            }
        });


        dbf.remove(iprvo);
        IpRangeHelper.updateL3NetworkIpversion(iprvo);

        CollectionUtils.safeForEach(exts, new ForEachFunction<IpRangeDeletionExtensionPoint>() {
            @Override
            public void run(IpRangeDeletionExtensionPoint arg) {
                arg.afterDeleteIpRange(inv);
            }
        });

        bus.reply(msg, reply);

    }

    private void handle(L3NetworkDeletionMsg msg) {
        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2NetworkVO = dbf.findByUuid(l3NetworkVO.getL2NetworkUuid(), L2NetworkVO.class);
        boolean isExistSystemL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.system, true)
                .eq(L3NetworkVO_.l2NetworkUuid, l2NetworkVO.getUuid()).isExists();
        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.clusterUuid)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkVO.getUuid()).listValues();
        if (isExistSystemL3) {
            if (clusterUuids != null && !clusterUuids.isEmpty()) {
                for (ServiceTypeExtensionPoint ext : pluginRgty.getExtensionList(ServiceTypeExtensionPoint.class)) {
                    List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid).in(HostVO_.clusterUuid, clusterUuids).listValues();
                    if (l2NetworkVO.getType().equals(L2NetworkConstant.VXLAN_NETWORK_TYPE) || l2NetworkVO.getType().equals(L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE)) {
                        ext.syncManagementServiceTypeExtensionPoint(hostUuids, "vxlan" + l2NetworkVO.getVirtualNetworkId(), null, true);
                    }
                    if (l2NetworkVO.getType().equals(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE) || l2NetworkVO.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)) {
                        ext.syncManagementServiceTypeExtensionPoint(hostUuids, l2NetworkVO.getPhysicalInterface(), l2NetworkVO.getVirtualNetworkId(), true);
                    }
                }
            }
        }

        L3NetworkInventory inv = L3NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        L3NetworkDeletionReply reply = new L3NetworkDeletionReply();
        bus.reply(msg, reply);
    }

    private void handle(ReturnIpMsg msg) {
        ReturnIpReply reply = new ReturnIpReply();
        new Retry<Void>() {
            String __name__ = String.format("return-ip-%s-for-l3-%s", msg.getUsedIpUuid(), msg.getL3NetworkUuid());

            @Override
            @RetryCondition(times = 6)
            protected Void call() {
                SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, msg.getUsedIpUuid()).hardDelete();
                return null;
            }
        }.run();
        logger.debug(String.format("Successfully released used ip[%s]", msg.getUsedIpUuid()));
        bus.reply(msg, reply);
    }

    private IpAllocatorType getIpAllocatorType(AllocateIpMsg msg) {
        if (msg.getAllocatorStrategy() != null) {
            return IpAllocatorType.valueOf(msg.getAllocatorStrategy());
        }

        if (msg.getIpVersion() == IPv6Constants.IPv4) {
            String ias = rcf.getResourceConfigValue(L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY, msg.getL3NetworkUuid(), String.class);
            if (ias != null) {
                return IpAllocatorType.valueOf(ias);
            }
            return RandomIpAllocatorStrategy.type;
        }

        String ias = rcf.getResourceConfigValue(L3NetworkGlobalConfig.IPV6_ALLOCATE_STRATEGY, msg.getL3NetworkUuid(), String.class);
        if (ias != null) {
            return IpAllocatorType.valueOf(ias);
        }
        return RandomIpv6AllocatorStrategy.type;
    }

    private void handle(AllocateIpMsg msg) {
        AllocateIpReply reply = new AllocateIpReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                IpAllocatorType strategyType = getIpAllocatorType(msg);
                IpAllocatorStrategy ias = l3NwMgr.getIpAllocatorStrategy(strategyType);
                UsedIpInventory ip = ias.allocateIp(msg);
                if (ip == null) {
                    String reason = msg.getRequiredIp() == null ?
                            String.format("no ip is available in this l3Network[name:%s, uuid:%s]", self.getName(), self.getUuid()) :
                            String.format("IP[%s] is not available", msg.getRequiredIp());
                    reply.setError(err(L3Errors.ALLOCATE_IP_ERROR,
                            "IP allocator strategy[%s] failed, because %s", strategyType, reason));
                    bus.reply(msg, reply);
                    chain.next();
                }

                logger.debug(String.format("Ip allocator strategy[%s] successfully allocates an ip[%s]", strategyType, printer.print(ip)));
                reply.setIpInventory(ip);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return "allocate-ip-of-l3-" + msg.getL3NetworkUuid();
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL3NetworkMsg) {
            handle((APIDeleteL3NetworkMsg) msg);
        } else if (msg instanceof APIDeleteIpRangeMsg) {
            handle((APIDeleteIpRangeMsg)msg);
        } else if (msg instanceof APIAddIpRangeMsg) {
            handle((APIAddIpRangeMsg) msg);
        } else if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            handle((APIAttachNetworkServiceToL3NetworkMsg) msg);
        } else if (msg instanceof APIDetachNetworkServiceFromL3NetworkMsg) {
            handle((APIDetachNetworkServiceFromL3NetworkMsg) msg);
        } else if (msg instanceof APIAddDnsToL3NetworkMsg) {
        	handle((APIAddDnsToL3NetworkMsg)msg);
        } else if (msg instanceof APIRemoveDnsFromL3NetworkMsg) {
            handle((APIRemoveDnsFromL3NetworkMsg) msg);
        } else if (msg instanceof APIChangeL3NetworkStateMsg) {
            handle((APIChangeL3NetworkStateMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            handle((APIAddIpRangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            handle((APIAddIpv6RangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeMsg) {
            handle((APIAddIpv6RangeMsg) msg);
        } else if (msg instanceof APIUpdateL3NetworkMsg) {
            handle((APIUpdateL3NetworkMsg) msg);
        } else if (msg instanceof APIGetFreeIpMsg) {
            handle((APIGetFreeIpMsg) msg);
        } else if (msg instanceof APIUpdateIpRangeMsg) {
            handle((APIUpdateIpRangeMsg) msg);
        } else if (msg instanceof APICheckIpAvailabilityMsg) {
            handle((APICheckIpAvailabilityMsg) msg);
        } else if (msg instanceof APISetL3NetworkRouterInterfaceIpMsg) {
            handle((APISetL3NetworkRouterInterfaceIpMsg) msg);
        } else if (msg instanceof APIGetL3NetworkRouterInterfaceIpMsg) {
            handle((APIGetL3NetworkRouterInterfaceIpMsg) msg);
        } else if (msg instanceof APIAddHostRouteToL3NetworkMsg) {
            handle((APIAddHostRouteToL3NetworkMsg) msg);
        } else if (msg instanceof APIRemoveHostRouteFromL3NetworkMsg) {
            handle((APIRemoveHostRouteFromL3NetworkMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected CheckIpAvailabilityReply checkIpAvailability(String ip) {
        CheckIpAvailabilityReply reply = new CheckIpAvailabilityReply();
        int ipversion = IPv6Constants.IPv4;
        if (IPv6NetworkUtils.isIpv6Address(ip)) {
            ipversion = IPv6Constants.IPv6;
        }
        SimpleQuery<IpRangeVO> rq = dbf.createQuery(IpRangeVO.class);
        rq.select(IpRangeVO_.startIp, IpRangeVO_.endIp, IpRangeVO_.gateway);
        rq.add(IpRangeVO_.l3NetworkUuid, Op.EQ, self.getUuid());
        rq.add(IpRangeVO_.ipVersion, Op.EQ, ipversion);
        List<Tuple> ts = rq.listTuple();

        List<String> addressPoolGateways = Q.New(AddressPoolVO.class)
                .select(AddressPoolVO_.gateway)
                .eq(AddressPoolVO_.l3NetworkUuid,self.getUuid()).listValues();

        boolean inRange = false;
        boolean isGateway = false;
        for (Tuple t : ts) {
            String sip = t.get(0, String.class);
            String eip = t.get(1, String.class);
            String gw = t.get(2, String.class);
            if (ip.equals(gw) && !addressPoolGateways.contains(gw)) {
                isGateway = true;
                break;
            }

            if (NetworkUtils.isInRange(ip, sip, eip)) {
                inRange = true;
                break;
            }
        }

        if (!inRange || isGateway) {
            // not an IP of this L3 or is a gateway
            reply.setAvailable(false);
            if (isGateway) {
                reply.setReason(IpNotAvailabilityReason.GATEWAY.toString());
            } else {
                reply.setReason(IpNotAvailabilityReason.NO_IN_RANGE.toString());
            }
            return reply;
        } else {
            SimpleQuery<UsedIpVO> q = dbf.createQuery(UsedIpVO.class);
            q.add(UsedIpVO_.l3NetworkUuid, Op.EQ, self.getUuid());
            q.add(UsedIpVO_.ip, Op.EQ, ip);
            if (q.isExists()) {
                reply.setAvailable(false);
                reply.setReason(IpNotAvailabilityReason.USED.toString());
            } else {
                reply.setAvailable(true);
            }

            return reply;
        }
    }

    private void handle(APICheckIpAvailabilityMsg msg) {
        APICheckIpAvailabilityReply reply = new APICheckIpAvailabilityReply();
        CheckIpAvailabilityReply r = checkIpAvailability(msg.getIp());
        reply.setAvailable(r.isAvailable());
        reply.setReason(r.getReason());
        bus.reply(msg, reply);
    }

    private void handle(final APIGetL3NetworkRouterInterfaceIpMsg msg) {
        APIGetL3NetworkRouterInterfaceIpReply reply = new APIGetL3NetworkRouterInterfaceIpReply();
        if (L3NetworkSystemTags.ROUTER_INTERFACE_IP.hasTag(msg.getL3NetworkUuid())) {
            reply.setRouterInterfaceIp(L3NetworkSystemTags.ROUTER_INTERFACE_IP.getTokenByResourceUuid(msg.getL3NetworkUuid(), L3NetworkSystemTags.ROUTER_INTERFACE_IP_TOKEN));
            bus.reply(msg, reply);
            return;
        }

        /* this api only support ipv4 */
        List<NormalIpRangeVO> ipRangeVOS = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
        if (ipRangeVOS == null || ipRangeVOS.isEmpty()) {
            reply.setRouterInterfaceIp(null);
            bus.reply(msg, reply);
        } else {
            reply.setRouterInterfaceIp(ipRangeVOS.get(0).getGateway());
            bus.reply(msg, reply);
        }
    }

    private void handle(final APISetL3NetworkRouterInterfaceIpMsg msg) {
        APISetL3NetworkRouterInterfaceIpEvent event = new APISetL3NetworkRouterInterfaceIpEvent(msg.getId());
        L3NetworkSystemTags.ROUTER_INTERFACE_IP.delete(msg.getRouterInterfaceIp());

        SystemTagCreator creator = L3NetworkSystemTags.ROUTER_INTERFACE_IP.newSystemTagCreator(msg.getL3NetworkUuid());
        creator.ignoreIfExisting = false;
        creator.inherent = false;
        creator.setTagByTokens(
                map(
                        e(L3NetworkSystemTags.ROUTER_INTERFACE_IP_TOKEN, msg.getRouterInterfaceIp())
                )
        );
        creator.create();

        bus.publish(event);
    }

    private void handle(APIDetachNetworkServiceFromL3NetworkMsg msg) {
        for (Map.Entry<String, List<String>> e : msg.getNetworkServices().entrySet()) {
            SimpleQuery<NetworkServiceL3NetworkRefVO> q = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
            q.add(NetworkServiceL3NetworkRefVO_.networkServiceProviderUuid, Op.EQ, e.getKey());
            q.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, self.getUuid());
            q.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.IN, e.getValue());
            List<NetworkServiceL3NetworkRefVO> refs = q.list();

            if (refs.isEmpty()) {
                logger.warn(String.format("no network service references found for the provider[uuid:%s] and L3 network[uuid:%s]",
                        e.getKey(), self.getUuid()));
            } else {
                dbf.removeCollection(refs, NetworkServiceL3NetworkRefVO.class);
            }

            logger.debug(String.format("successfully detached network service provider[uuid:%s] to l3network[uuid:%s, name:%s] with services%s", e.getKey(), self.getUuid(), self.getName(), e.getValue()));
        }

        self = dbf.reload(self);
        APIDetachNetworkServiceFromL3NetworkEvent evt = new APIDetachNetworkServiceFromL3NetworkEvent(msg.getId());
        evt.setInventory(L3NetworkInventory.valueOf(self));
        bus.publish(evt);
    }

    private void handle(APIUpdateIpRangeMsg msg) {
        IpRangeVO vo = dbf.findByUuid(msg.getUuid(), IpRangeVO.class);
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
        APIUpdateIpRangeEvent evt = new APIUpdateIpRangeEvent(msg.getId());
        evt.setInventory(IpRangeInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIGetFreeIpMsg msg) {
        APIGetFreeIpReply reply = new APIGetFreeIpReply();

        List<IpRangeVO> ipRangeVOs = new ArrayList<>();
        List<Integer> ipVersions = new ArrayList<>();
        if(msg.getIpVersion()!=null){
            if(msg.getIpVersion()== IPv6Constants.DUAL_STACK){
                ipVersions.add(IPv6Constants.IPv4);
                ipVersions.add(IPv6Constants.IPv6);
            }else{
                ipVersions.add(msg.getIpVersion());
            }
        }

        if (msg.getIpRangeUuid() != null) {
            IpRangeVO ipr = dbf.findByUuid(msg.getIpRangeUuid(), IpRangeVO.class);
            ipRangeVOs.add(ipr);
        } else {
            if (msg.getIpRangeType() == null) {
                List<IpRangeVO> tempIpRangeVO = Q.New(IpRangeVO.class)
                        .eq(IpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .in(IpRangeVO_.ipVersion, ipVersions)
                        .list();
                ipRangeVOs.addAll(tempIpRangeVO);
            } else if (msg.getIpRangeType().equals(IpRangeType.Normal.toString())) {
                List<IpRangeVO> tempIpRangeVO = Q.New(NormalIpRangeVO.class)
                        .eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .in(NormalIpRangeVO_.ipVersion, ipVersions)
                        .list();
                ipRangeVOs.addAll(tempIpRangeVO);

            } else {
                List<IpRangeVO> tempIpRangeVO = Q.New(AddressPoolVO.class)
                        .eq(AddressPoolVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                        .in(AddressPoolVO_.ipVersion, ipVersions)
                        .list();
                ipRangeVOs.addAll(tempIpRangeVO);
            }
        }

        List<FreeIpInventory> freeIpInventorys = new ArrayList<FreeIpInventory>();
        int limit = msg.getLimit();
        String start = msg.getStart();
        for ( IpRangeVO ipRangeVO : ipRangeVOs) {
            if( msg.getStart() == null){
                if ( ipRangeVO.getIpVersion() == IPv6Constants.IPv6) {
                    start = "::";
                } else {
                    start = "0.0.0.0";
                }
            }
            List<FreeIpInventory> tempFreeIpInventorys = IpRangeHelper.getFreeIp(ipRangeVO, limit,start);
            freeIpInventorys.addAll(tempFreeIpInventorys);
            if (freeIpInventorys.size() >= msg.getLimit()) {
                break;
            }
            limit -= freeIpInventorys.size();
        }
        reply.setInventories(freeIpInventorys);

        bus.reply(msg, reply);
    }

    private void handle(APIUpdateL3NetworkMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (msg.getSystem() != null && msg.getCategory() != null) {
                self.setSystem(msg.getSystem());
                self.setCategory(L3NetworkCategory.valueOf(msg.getCategory()));
                update = true;
        }
        if (msg.getDnsDomain() != null) {
            self.setDnsDomain(msg.getDnsDomain());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateL3NetworkEvent evt = new APIUpdateL3NetworkEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }


    private void handle(APIAddIpRangeByNetworkCidrMsg msg) {
        List<IpRangeInventory> iprs = IpRangeInventory.fromMessage(msg);
        List<IpRangeInventory> ret = new ArrayList<>();
        for (IpRangeInventory ipr : iprs) {
            IpRangeFactory factory = l3NwMgr.getIpRangeFactory(ipr.getIpRangeType());
            IpRangeInventory inv = factory.createIpRange(ipr, msg);
            setIpRangeSharedResource(msg.getL3NetworkUuid(), inv.getUuid());
            ret.add(inv);
        }
        tagMgr.createTagsFromAPICreateMessage(msg, iprs.get(0).getL3NetworkUuid(), L3NetworkVO.class.getSimpleName());
        APIAddIpRangeByNetworkCidrEvent evt = new APIAddIpRangeByNetworkCidrEvent(msg.getId());
        evt.setInventory(ret.get(0));
        evt.setInventories(ret);
        bus.publish(evt);
    }

    private void handle(APIAddIpv6RangeByNetworkCidrMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        IpRangeFactory factory = l3NwMgr.getIpRangeFactory(ipr.getIpRangeType());
        IpRangeInventory inv = factory.createIpRange(ipr, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, inv.getL3NetworkUuid(), L3NetworkVO.class.getSimpleName());;

        setIpRangeSharedResource(msg.getL3NetworkUuid(), inv.getUuid());

        APIAddIpRangeByNetworkCidrEvent evt = new APIAddIpRangeByNetworkCidrEvent(msg.getId());
        evt.setInventory(inv);
        evt.setInventories(Collections.singletonList(inv));
        bus.publish(evt);
    }

    private void handle(APIAddIpv6RangeMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        IpRangeFactory factory = l3NwMgr.getIpRangeFactory(ipr.getIpRangeType());
        IpRangeInventory inv = factory.createIpRange(ipr, msg);

        tagMgr.createTagsFromAPICreateMessage(msg, inv.getL3NetworkUuid(), L3NetworkVO.class.getSimpleName());

        setIpRangeSharedResource(msg.getL3NetworkUuid(), inv.getUuid());

        APIAddIpRangeEvent evt = new APIAddIpRangeEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }


    private void handle(APIChangeL3NetworkStateMsg msg) {
        if (L3NetworkStateEvent.enable.toString().equals(msg.getStateEvent())) {
            self.setState(L3NetworkState.Enabled);
        } else {
            self.setState(L3NetworkState.Disabled);
        }

        self = dbf.updateAndRefresh(self);

        APIChangeL3NetworkStateEvent evt = new APIChangeL3NetworkStateEvent(msg.getId());
        evt.setInventory(L3NetworkInventory.valueOf(self));
        bus.publish(evt);
    }



    private void handle(final APIRemoveDnsFromL3NetworkMsg msg) {
        final APIRemoveDnsFromL3NetworkEvent evt = new APIRemoveDnsFromL3NetworkEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-dns-%s-from-l3-%s", msg.getDns(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "remove-dns-from-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
                        q.add(L3NetworkDnsVO_.dns, Op.EQ, msg.getDns());
                        q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
                        L3NetworkDnsVO dns = q.find();
                        if (dns != null) {
                            //TODO: create extension points
                            dbf.remove(dns);
                        }
                        trigger.next();
                    }
                });

                if (L3NetworkConstant.L3_BASIC_NETWORK_TYPE.equals(self.getType()) && !self.getNetworkServices().isEmpty()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "remove-dns-from-backend";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            RemoveDnsMsg rmsg = new RemoveDnsMsg();
                            rmsg.setDns(msg.getDns());
                            rmsg.setL3NetworkUuid(self.getUuid());
                            bus.makeLocalServiceId(rmsg, NetworkServiceConstants.DNS_SERVICE_ID);
                            bus.send(rmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(L3NetworkInventory.valueOf(dbf.reload(self)));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIAddDnsToL3NetworkMsg msg) {
        final APIAddDnsToL3NetworkEvent evt = new APIAddDnsToL3NetworkEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-dns-%s-to-l3-%s", msg.getDns(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-dns-to-db";

                    L3NetworkDnsVO dnsvo;
                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        dnsvo = new L3NetworkDnsVO();
                        dnsvo.setDns(msg.getDns());
                        dnsvo.setL3NetworkUuid(self.getUuid());
                        dnsvo = dbf.persist(dnsvo);
                        s = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (s) {
                            dbf.remove(dnsvo);
                        }
                        trigger.rollback();
                    }
                });

                if (L3NetworkConstant.L3_BASIC_NETWORK_TYPE.equals(self.getType()) && !self.getNetworkServices().isEmpty()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "apply-to-backend";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AddDnsMsg amsg = new AddDnsMsg();
                            amsg.setL3NetworkUuid(self.getUuid());
                            amsg.setDns(msg.getDns());
                            bus.makeLocalServiceId(amsg, NetworkServiceConstants.DNS_SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.reload(self);
                        evt.setInventory(L3NetworkInventory.valueOf(self));
                        logger.debug(String.format("successfully added dns[%s] to L3Network[uuid:%s, name:%s]", msg.getDns(), self.getUuid(), self.getName()));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
	}

	private void handle(final AttachNetworkServiceToL3Msg msg) {
        MessageReply reply = new MessageReply();
        for (Map.Entry<String, List<String>> e : msg.getNetworkServices().entrySet()) {
            for (String nsType : e.getValue()) {
                NetworkServiceL3NetworkRefVO ref = new NetworkServiceL3NetworkRefVO();
                ref.setL3NetworkUuid(self.getUuid());
                ref.setNetworkServiceProviderUuid(e.getKey());
                ref.setNetworkServiceType(nsType);
                dbf.persist(ref);
            }
            logger.debug(String.format("successfully attached network service provider[uuid:%s] to l3network[uuid:%s, name:%s] with services%s", e.getKey(), self.getUuid(), self.getName(), e.getValue()));
        }
        bus.reply(msg, reply);
    }

	private void handle(APIAttachNetworkServiceToL3NetworkMsg msg) {
        APIAttachNetworkServiceToL3NetworkEvent evt = new APIAttachNetworkServiceToL3NetworkEvent(msg.getId());
        AttachNetworkServiceToL3Msg amsg = new AttachNetworkServiceToL3Msg();
        amsg.setL3NetworkUuid(msg.getL3NetworkUuid());
        amsg.setNetworkServices(msg.getNetworkServices());

        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());
        bus.send(amsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    self = dbf.reload(self);
                    evt.setInventory(L3NetworkInventory.valueOf(self));
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
	}


    private void doDeleteIpRange(APIDeleteIpRangeMsg msg, Completion completion) {
        IpRangeVO vo = dbf.findByUuid(msg.getUuid(), IpRangeVO.class);
        final String issuer = IpRangeVO.class.getSimpleName();
        final List<IpRangeInventory> ctx = IpRangeInventory.valueOf(Arrays.asList(vo));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-ip-range-%s", vo.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                completion.success();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void syncManagementServiceTypeWhileDelete(ServiceTypeExtensionPoint ext, L2NetworkVO l2NetworkVO, List<String> hostUuids) {
        String l2NetworkType = l2NetworkVO.getType();
        switch (l2NetworkType) {
            case L2NetworkConstant.VXLAN_NETWORK_TYPE:
                ext.syncManagementServiceTypeExtensionPoint(hostUuids, "vxlan" + l2NetworkVO.getVirtualNetworkId(), null, true);
                break;

            case L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE:
            case L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE:
            case L2NetworkConstant.L2_VLAN_NETWORK_TYPE:
                ext.syncManagementServiceTypeExtensionPoint(hostUuids, l2NetworkVO.getPhysicalInterface(), l2NetworkVO.getVirtualNetworkId(), true);
                break;

            default:
                break;
        }
    }

    private void handle(APIDeleteIpRangeMsg msg) {
        self = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        final APIDeleteIpRangeEvent evt = new APIDeleteIpRangeEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
                    @Override
                    public String getSyncSignature() {
                        return getSyncId();
                    }

                    @Override
                    public String getName() {
                        return "delete-ip-range-" + msg.getIpRangeUuid();
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        doDeleteIpRange(msg, new Completion(chain) {
                            @Override
                            public void success() {
                                bus.publish(evt);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                evt.setError(errorCode);
                                bus.publish(evt);
                                chain.next();
                            }
                        });
                    }
                }
        );
    }

    private void doDeleteL3Network(APIDeleteL3NetworkMsg msg, Completion completion) {
        final String issuer = L3NetworkVO.class.getSimpleName();
        final List<L3NetworkInventory> ctx = L3NetworkInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2NetworkVO = dbf.findByUuid(l3NetworkVO.getL2NetworkUuid(), L2NetworkVO.class);
        chain.setName(String.format("delete-l3-network-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                boolean isExistSystemL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.system, true)
                        .eq(L3NetworkVO_.l2NetworkUuid, l2NetworkVO.getUuid()).isExists();
                List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.clusterUuid)
                        .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkVO.getUuid()).listValues();
                if (!isExistSystemL3) {
                    if (clusterUuids != null && !clusterUuids.isEmpty()) {
                        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                                .in(HostVO_.clusterUuid, clusterUuids).listValues();
                        for (ServiceTypeExtensionPoint ext : pluginRgty.getExtensionList(ServiceTypeExtensionPoint.class)) {
                            syncManagementServiceTypeWhileDelete(ext, l2NetworkVO, hostUuids);
                        }
                    }
                }
                completion.success();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
    private void handle(APIDeleteL3NetworkMsg msg) {
        final APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                doDeleteL3Network(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return "delete-l3-network-" + msg.getL3NetworkUuid();
            }
        });
    }

    private void handle(APIAddHostRouteToL3NetworkMsg msg) {
        final APIAddHostRouteToL3NetworkEvent evt = new APIAddHostRouteToL3NetworkEvent(msg.getId());

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("add-hostroute-prefix-%s-to-l3-%s", msg.getPrefix(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-hostroute-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        L3NetworkHostRouteVO vo = new L3NetworkHostRouteVO();
                        vo.setL3NetworkUuid(msg.getL3NetworkUuid());
                        vo.setPrefix(msg.getPrefix());
                        vo.setNexthop(msg.getNexthop());
                        vo = dbf.persist(vo);
                        data.put(L3NetworkConstant.Param.L3_HOSTROUTE_VO, vo);
                        data.put(L3NetworkConstant.Param.L3_HOSTROUTE_SUCCESS, true);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        boolean flag = (boolean) data.get(L3NetworkConstant.Param.L3_HOSTROUTE_SUCCESS);
                        if (flag) {
                            L3NetworkHostRouteVO vo = (L3NetworkHostRouteVO) data.get(L3NetworkConstant.Param.L3_HOSTROUTE_VO);
                            dbf.remove(vo);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        AddHostRouteMsg amsg = new AddHostRouteMsg();
                        amsg.setL3NetworkUuid(self.getUuid());
                        amsg.setPrefix(msg.getPrefix());
                        amsg.setNexthop(msg.getNexthop());
                        bus.makeLocalServiceId(amsg, NetworkServiceConstants.HOSTROUTE_SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        self = dbf.reload(self);
                        evt.setInventory(L3NetworkInventory.valueOf(self));
                        logger.debug(String.format("successfully added hostRoute prefix [%s] nexthop [%s] to L3Network[uuid:%s, name:%s]",
                                msg.getPrefix(), msg.getNexthop(), self.getUuid(), self.getName()));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIRemoveHostRouteFromL3NetworkMsg msg) {
        final APIRemoveHostRouteFromL3NetworkEvent evt = new APIRemoveHostRouteFromL3NetworkEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-hostroute-prefix-%s-from-l3-%s", msg.getPrefix(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "remove-hostroute-from-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SimpleQuery<L3NetworkHostRouteVO> q = dbf.createQuery(L3NetworkHostRouteVO.class);
                        q.add(L3NetworkHostRouteVO_.prefix, Op.EQ, msg.getPrefix());
                        q.add(L3NetworkHostRouteVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
                        L3NetworkHostRouteVO hostRouteVO = q.find();
                        if (hostRouteVO != null) {
                            dbf.remove(hostRouteVO);
                        }
                        trigger.next();
                    }
                });

                if (!self.getNetworkServices().isEmpty()) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "remove-hostroute-from-backend";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            RemoveHostRouteMsg rmsg = new RemoveHostRouteMsg();
                            rmsg.setL3NetworkUuid(self.getUuid());
                            rmsg.setPrefix(msg.getPrefix());
                            bus.makeLocalServiceId(rmsg, NetworkServiceConstants.HOSTROUTE_SERVICE_ID);
                            bus.send(rmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(L3NetworkInventory.valueOf(dbf.reload(self)));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }
}
