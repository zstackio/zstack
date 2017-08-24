package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.identity.AccountManager;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    private IpRangeInventory createIpRange(APICreateMessage msg, IpRangeInventory ipr) {
        IpRangeVO vo = new SQLBatchWithReturn<IpRangeVO>() {
            @Override
            protected IpRangeVO scripts() {
                IpRangeVO vo = new IpRangeVO();
                vo.setUuid(ipr.getUuid() == null ? Platform.getUuid() : ipr.getUuid());
                vo.setDescription(ipr.getDescription());
                vo.setEndIp(ipr.getEndIp());
                vo.setGateway(ipr.getGateway());
                vo.setL3NetworkUuid(ipr.getL3NetworkUuid());
                vo.setName(ipr.getName());
                vo.setNetmask(ipr.getNetmask());
                vo.setStartIp(ipr.getStartIp());
                vo.setNetworkCidr(ipr.getNetworkCidr());
                dbf.getEntityManager().persist(vo);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(vo);

                acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), IpRangeVO.class);

                return vo;
            }
        }.execute();

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), IpRangeVO.class.getSimpleName());

        IpRangeInventory inv = IpRangeInventory.valueOf(vo);
        logger.debug(String.format("Successfully added ip range: %s", JSONObjectUtil.toJsonString(inv)));
        return inv;
    }

    private void handle(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        ipr = createIpRange(msg, ipr);

        final IpRangeInventory finalIpr = ipr;
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAddIpRangeExtensionPoint.class), new ForEachFunction<AfterAddIpRangeExtensionPoint>() {
            @Override
            public void run(AfterAddIpRangeExtensionPoint ext) {
                ext.afterAddIpRange(finalIpr);
            }
        });

        APIAddIpRangeEvent evt = new APIAddIpRangeEvent(msg.getId());
        evt.setInventory(ipr);
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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CheckIpAvailabilityMsg msg) {
        CheckIpAvailabilityReply reply = new CheckIpAvailabilityReply();
        reply.setAvailable(checkIpAvailability(msg.getIp()));
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

        CollectionUtils.safeForEach(exts, new ForEachFunction<IpRangeDeletionExtensionPoint>() {
            @Override
            public void run(IpRangeDeletionExtensionPoint arg) {
                arg.afterDeleteIpRange(inv);
            }
        });

        bus.reply(msg, reply);
    }

    private void handle(L3NetworkDeletionMsg msg) {
        L3NetworkInventory inv = L3NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        L3NetworkDeletionReply reply = new L3NetworkDeletionReply();
        bus.reply(msg, reply);
    }

    private void handle(ReturnIpMsg msg) {
        ReturnIpReply reply = new ReturnIpReply();
        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, msg.getUsedIpUuid()).hardDelete();
        logger.debug(String.format("Successfully released used ip[%s]", msg.getUsedIpUuid()));
        bus.reply(msg, reply);
    }



    private void handle(AllocateIpMsg msg) {
        IpAllocatorType strategyType = msg.getAllocatorStrategy() == null ? RandomIpAllocatorStrategy.type : IpAllocatorType.valueOf(msg.getAllocatorStrategy());
        IpAllocatorStrategy ias = l3NwMgr.getIpAllocatorStrategy(strategyType);
        AllocateIpReply reply = new AllocateIpReply();
        UsedIpInventory ip = ias.allocateIp(msg);
        if (ip == null) {
            String reason = msg.getRequiredIp() == null ?
                    String.format("no ip is available in this l3Network[name:%s, uuid:%s]", self.getName(), self.getUuid()) :
                    String.format("IP[%s] is not available", msg.getRequiredIp());
            reply.setError(errf.instantiateErrorCode(L3Errors.ALLOCATE_IP_ERROR,
                    String.format("IP allocator strategy[%s] failed, because %s", strategyType, reason)));
        } else {
            logger.debug(String.format("Ip allocator strategy[%s] successfully allocates an ip[%s]", strategyType, printer.print(ip)));
            reply.setIpInventory(ip);
        }

        bus.reply(msg, reply);
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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected boolean checkIpAvailability(String ip) {
        SimpleQuery<IpRangeVO> rq = dbf.createQuery(IpRangeVO.class);
        rq.select(IpRangeVO_.startIp, IpRangeVO_.endIp, IpRangeVO_.gateway);
        rq.add(IpRangeVO_.l3NetworkUuid, Op.EQ, self.getUuid());
        List<Tuple> ts = rq.listTuple();

        boolean inRange = false;
        boolean isGateway = false;
        for (Tuple t : ts) {
            String sip = t.get(0, String.class);
            String eip = t.get(1, String.class);
            String gw = t.get(2, String.class);
            if (ip.equals(gw)) {
                isGateway = true;
                break;
            }

            if (NetworkUtils.isIpv4InRange(ip, sip, eip)) {
                inRange = true;
                break;
            }
        }

        if (!inRange || isGateway) {
            // not an IP of this L3 or is a gateway
            return false;
        } else {
            SimpleQuery<UsedIpVO> q = dbf.createQuery(UsedIpVO.class);
            q.add(UsedIpVO_.l3NetworkUuid, Op.EQ, self.getUuid());
            q.add(UsedIpVO_.ip, Op.EQ, ip);
            return !q.isExists();
        }
    }

    private void handle(APICheckIpAvailabilityMsg msg) {
        APICheckIpAvailabilityReply reply = new APICheckIpAvailabilityReply();
        reply.setAvailable(checkIpAvailability(msg.getIp()));
        bus.reply(msg, reply);
    }

    private void handle(final APIGetL3NetworkRouterInterfaceIpMsg msg) {
        APIGetL3NetworkRouterInterfaceIpReply reply = new APIGetL3NetworkRouterInterfaceIpReply();
        if (L3NetworkSystemTags.ROUTER_INTERFACE_IP.hasTag(msg.getL3NetworkUuid())) {
            reply.setRouterInterfaceIp(L3NetworkSystemTags.ROUTER_INTERFACE_IP.getTokenByResourceUuid(msg.getL3NetworkUuid(), L3NetworkSystemTags.ROUTER_INTERFACE_IP_TOKEN));
            bus.reply(msg, reply);
            return;
        }

        List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid()).list();
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

    private List<FreeIpInventory> getFreeIp(final IpRangeVO ipr, int limit, String start) {
        SimpleQuery<UsedIpVO> q = dbf.createQuery(UsedIpVO.class);
        q.select(UsedIpVO_.ip);
        q.add(UsedIpVO_.ipRangeUuid, Op.EQ, ipr.getUuid());

        List<String> used = q.listValue();

        List<String> spareIps = NetworkUtils.getFreeIpInRange(ipr.getStartIp(), ipr.getEndIp(), used, limit, start);
        return CollectionUtils.transformToList(spareIps, new Function<FreeIpInventory, String>() {
            @Override
            public FreeIpInventory call(String arg) {
                FreeIpInventory f = new FreeIpInventory();
                f.setGateway(ipr.getGateway());
                f.setIp(arg);
                f.setNetmask(ipr.getNetmask());
                f.setIpRangeUuid(ipr.getUuid());
                return f;
            }
        });
    }

    private void handle(APIGetFreeIpMsg msg) {
        APIGetFreeIpReply reply = new APIGetFreeIpReply();

        if (msg.getIpRangeUuid() != null) {
            final IpRangeVO ipr = dbf.findByUuid(msg.getIpRangeUuid(), IpRangeVO.class);
            List<FreeIpInventory> free = getFreeIp(ipr, msg.getLimit(),msg.getStart());
            reply.setInventories(free);
        } else {
            SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
            q.add(IpRangeVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            List<IpRangeVO> iprs = q.list();
            List<FreeIpInventory> res = new ArrayList<FreeIpInventory>();
            int limit = msg.getLimit();
            for (IpRangeVO ipr : iprs) {
                List<FreeIpInventory> i = getFreeIp(ipr, limit,msg.getStart());
                res.addAll(i);
                if (res.size() >= msg.getLimit()) {
                    break;
                }
                limit -= res.size();
            }
            reply.setInventories(res);
        }

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
        if (msg.getSystem() != null) {
            self.setSystem(msg.getSystem());
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
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        ipr = createIpRange(msg, ipr);
        APIAddIpRangeByNetworkCidrEvent evt = new APIAddIpRangeByNetworkCidrEvent(msg.getId());
        evt.setInventory(ipr);
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
                if (!self.getNetworkServices().isEmpty()) {
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

                if (!self.getNetworkServices().isEmpty()) {
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

	private void handle(APIAttachNetworkServiceToL3NetworkMsg msg) {
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

    	self = dbf.reload(self);
    	APIAttachNetworkServiceToL3NetworkEvent evt = new APIAttachNetworkServiceToL3NetworkEvent(msg.getId());
    	evt.setInventory(L3NetworkInventory.valueOf(self));
    	bus.publish(evt);
	}


    private void handle(APIDeleteIpRangeMsg msg) {
        IpRangeVO vo = dbf.findByUuid(msg.getUuid(), IpRangeVO.class);
        final APIDeleteIpRangeEvent evt = new APIDeleteIpRangeEvent(msg.getId());
        final String issuer = IpRangeVO.class.getSimpleName();
        final List<IpRangeInventory> ctx = IpRangeInventory.valueOf(Arrays.asList(vo));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-ip-range-%s", msg.getUuid()));
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
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APIDeleteL3NetworkMsg msg) {
        final APIDeleteL3NetworkEvent evt = new APIDeleteL3NetworkEvent(msg.getId());
        final String issuer = L3NetworkVO.class.getSimpleName();
        final List<L3NetworkInventory> ctx = L3NetworkInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
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
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }
}
