package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.inventory.InventoryFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.*;
import org.zstack.header.network.l2.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2NoVlanNetwork implements L2Network {
    private static final CLogger logger = Utils.getLogger(L2NoVlanNetwork.class);
    
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

    protected L2NetworkVO self;

    public L2NoVlanNetwork(L2NetworkVO self) {
        this.self = self;
    }

    @Override
    public void deleteHook() {
    }

    protected L2NetworkInventory getSelfInventory() {
        return L2NetworkInventory.valueOf(self);
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
        SimpleQuery<L2NetworkClusterRefVO> query = dbf.createQuery(L2NetworkClusterRefVO.class);
        query.add(L2NetworkClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        query.add(L2NetworkClusterRefVO_.l2NetworkUuid, Op.EQ, msg.getL2NetworkUuid());
        L2NetworkClusterRefVO rvo = query.find();
        if (rvo != null) {
            dbf.remove(rvo);
        }

        DetachL2NetworkFromClusterReply reply = new DetachL2NetworkFromClusterReply();
        bus.reply(msg, reply);
    }

    private void handle(final PrepareL2NetworkOnHostMsg msg) {
        final PrepareL2NetworkOnHostReply reply = new PrepareL2NetworkOnHostReply();
        prepareL2NetworkOnHosts(Arrays.asList(msg.getHost()), new Completion() {
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

    private void handle(final CheckL2NetworkOnHostMsg msg) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.hypervisorType);
        q.add(HostVO_.uuid, Op.EQ, msg.getHostUuid());
        String htype = q.findValue();
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        final CheckL2NetworkOnHostReply reply = new CheckL2NetworkOnHostReply();
        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType);
        ext.check(getSelfInventory(), msg.getHostUuid(), new Completion() {
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

    private void handle(L2NetworkDeletionMsg msg) {
        L2NetworkInventory inv = L2NetworkInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        L2NetworkDeletionReply reply = new L2NetworkDeletionReply();
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteL2NetworkMsg) {
            handle((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            handle((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            handle((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkMsg) {
            handle((APIUpdateL2NetworkMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateL2NetworkMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateL2NetworkEvent evt = new APIUpdateL2NetworkEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(final APIDetachL2NetworkFromClusterMsg msg) {
        final APIDetachL2NetworkFromClusterEvent evt = new APIDetachL2NetworkFromClusterEvent(msg.getId());

        String issuer = L2NetworkVO.class.getSimpleName();
        List<L2NetworkDetachStruct> ctx = new ArrayList<L2NetworkDetachStruct>();
        L2NetworkDetachStruct struct = new L2NetworkDetachStruct();
        struct.setClusterUuid(msg.getClusterUuid());
        struct.setL2NetworkUuid(msg.getL2NetworkUuid());
        ctx.add(struct);
        casf.asyncCascade(L2NetworkConstant.DETACH_L2NETWORK_CODE, issuer, ctx, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully detached L2Network[uuid:%s] to cluster [uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.reload(self);
                evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    protected void realizeNetwork(String hostUuid, String htype, Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(self.getType());

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType);
        ext.realize(getSelfInventory(), hostUuid, completion);
    }

    private void prepareL2NetworkOnHosts(final List<HostInventory> hosts, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-%s-on-hosts", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<CheckNetworkPhysicalInterfaceMsg> cmsgs = new ArrayList<CheckNetworkPhysicalInterfaceMsg>();
                for (HostInventory h : hosts) {
                    CheckNetworkPhysicalInterfaceMsg cmsg = new CheckNetworkPhysicalInterfaceMsg();
                    cmsg.setHostUuid(h.getUuid());
                    cmsg.setPhysicalInterface(self.getPhysicalInterface());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, h.getUuid());
                    cmsgs.add(cmsg);
                }

                if (cmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.send(cmsgs, new CloudBusListCallBack() {
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
                realizeNetwork(host.getUuid(), host.getHypervisorType(), new Completion() {
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

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
        final APIAttachL2NetworkToClusterEvent evt = new APIAttachL2NetworkToClusterEvent(msg.getId());
        SimpleQuery<L2NetworkClusterRefVO> rq = dbf.createQuery(L2NetworkClusterRefVO.class);
        rq.add(L2NetworkClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        rq.add(L2NetworkClusterRefVO_.l2NetworkUuid, Op.EQ, msg.getL2NetworkUuid());
        long count = rq.count();
        if (count != 0) {
            evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
            bus.publish(evt);
            return;
        }

        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.add(HostVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        query.add(HostVO_.state, Op.NOT_IN, HostState.PreMaintenance, HostState.Maintenance);
        query.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        final List<HostVO> hosts = query.list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);

        prepareL2NetworkOnHosts(hvinvs, new Completion(msg) {
            @Override
            public void success() {
                L2NetworkClusterRefVO rvo = new L2NetworkClusterRefVO();
                rvo.setClusterUuid(msg.getClusterUuid());
                rvo.setL2NetworkUuid(self.getUuid());
                dbf.persist(rvo);
                logger.debug(String.format("successfully attached L2Network[uuid:%s] to cluster [uuid:%s]", self.getUuid(), msg.getClusterUuid()));
                self = dbf.findByUuid(self.getUuid(), L2NetworkVO.class);
                evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setErrorCode(errf.instantiateErrorCode(L2Errors.ATTACH_ERROR, errorCode));
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDeleteL2NetworkMsg msg) {
        final APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
        final String issuer = L2NetworkVO.class.getSimpleName();
        final List<L2NetworkInventory> ctx = L2NetworkInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-l2Network-%s", msg.getL2NetworkUuid()));
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
