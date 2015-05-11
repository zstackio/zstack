package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkEvent;
import org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    private L3NetworkVO self;

    public L3BasicNetwork(L3NetworkVO vo) {
        this.self = vo;
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
        vo = dbf.persistAndRefresh(vo);

        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vo.getUuid(), IpRangeVO.class);
        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), IpRangeVO.class.getSimpleName());

        IpRangeInventory inv = IpRangeInventory.valueOf(vo);
        logger.debug(String.format("Successfully added ip range: %s", JSONObjectUtil.toJsonString(inv)));
        return inv;
    }

    private void handle(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr = IpRangeInventory.fromMessage(msg);
        ipr = createIpRange(msg, ipr);
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
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(IpRangeDeletionMsg msg) {
        IpRangeDeletionReply reply = new IpRangeDeletionReply();
        IpRangeVO iprvo = dbf.findByUuid(msg.getIpRangeUuid(), IpRangeVO.class);
        deleteIpRangeHook(IpRangeInventory.valueOf(iprvo));
        bus.reply(msg, reply);
    }

    // for inheriting
    protected void deleteIpRangeHook(IpRangeInventory ipRangeInventory) {
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
        dbf.removeByPrimaryKey(msg.getUsedIpUuid(), UsedIpVO.class);
        logger.debug(String.format("Successfully released used ip[%s]", msg.getUsedIpUuid()));
        bus.reply(msg, reply);
    }

    private void handle(AllocateIpMsg msg) {
        IpAllocatorType strategyType = msg.getAllocatorStrategy() == null ? RandomIpAllocatorStrategy.type : IpAllocatorType.valueOf(msg.getAllocatorStrategy());
        IpAllocatorStrategy ias = l3NwMgr.getIpAllocatorStrategy(strategyType);
        AllocateIpReply reply = new AllocateIpReply();
        UsedIpInventory ip = ias.allocateIp(msg);
        if (ip == null) {
            reply.setError(errf.instantiateErrorCode(L3Errors.ALLOCATE_IP_ERROR, String.format("IP allocator strategy[%s] returns nothing, because no ip is available in this l3Network[name:%s, uuid:%s]", strategyType, self.getName(), self.getUuid())));
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
        	handle((APIAttachNetworkServiceToL3NetworkMsg)msg);
        } else if (msg instanceof APIAddDnsToL3NetworkMsg) {
        	handle((APIAddDnsToL3NetworkMsg)msg);
        } else if (msg instanceof APIRemoveDnsFromL3NetworkMsg) {
            handle((APIRemoveDnsFromL3NetworkMsg) msg);
        } else if (msg instanceof APIChangeL3NetworkStateMsg) {
            handle((APIChangeL3NetworkStateMsg) msg);
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            handle((APIAddIpRangeByNetworkCidrMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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



    private void handle(APIRemoveDnsFromL3NetworkMsg msg) {
        SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
        q.add(L3NetworkDnsVO_.dns, Op.EQ, msg.getDns());
        q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        L3NetworkDnsVO dns = q.find();
        APIRemoveDnsFromL3NetworkEvent evt = new APIRemoveDnsFromL3NetworkEvent(msg.getId());
        if (dns != null) {
            //TODO: create extension points
            dbf.remove(dns);
        }
        evt.setInventory(L3NetworkInventory.valueOf(dbf.reload(self)));
        bus.publish(evt);
    }

    private void handle(APIAddDnsToL3NetworkMsg msg) {
    	L3NetworkDnsVO dnsvo = new L3NetworkDnsVO();
    	dnsvo.setDns(msg.getDns());
    	dnsvo.setL3NetworkUuid(self.getUuid());
    	dbf.persist(dnsvo);

    	APIAddDnsToL3NetworkEvent evt = new APIAddDnsToL3NetworkEvent(msg.getId());
    	self = dbf.reload(self);
    	evt.setInventory(L3NetworkInventory.valueOf(self));
    	logger.debug(String.format("successfully added dns[%s] to L3Network[uuid:%s, name:%s]", msg.getDns(), self.getUuid(), self.getName()));
    	bus.publish(evt);
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
    	
    	self = dbf.findByUuid(self.getUuid(), L3NetworkVO.class);
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
                evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
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
                evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.publish(evt);
            }
        }).start();
    }
}
