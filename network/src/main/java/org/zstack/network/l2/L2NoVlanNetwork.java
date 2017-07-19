package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.inventory.InventoryFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.identity.SharedResourceVO;
import org.zstack.header.identity.SharedResourceVO_;
import org.zstack.header.message.*;
import org.zstack.header.network.l2.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

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
    @Autowired
    protected ThreadFacade thdf;

    protected L2NetworkVO self;

    public L2NoVlanNetwork(L2NetworkVO self) {
        this.self = self;
    }

    public L2NoVlanNetwork() {
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
        } else if (msg instanceof DeleteL2NetworkMsg) {
            handle((DeleteL2NetworkMsg) msg);
        } else if (msg instanceof L2NetworkDetachFromClusterMsg) {
            handle((L2NetworkDetachFromClusterMsg) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(L2NetworkDetachFromClusterMsg msg) {
        L2NetworkDetachFromClusterReply reply = new L2NetworkDetachFromClusterReply();

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
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DeleteL2NetworkMsg msg) {
        DeleteL2NetworkReply reply = new DeleteL2NetworkReply();
        final String issuer = L2NetworkVO.class.getSimpleName();
        final List<L2NetworkInventory> ctx = L2NetworkInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-l2Network-%s", msg.getL2NetworkUuid()));
        if (msg.isForceDelete() == false) {
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
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                bus.reply(msg, reply);
            }
        }).start();
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
        prepareL2NetworkOnHosts(Arrays.asList(msg.getHost()), new Completion(msg) {
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
        ext.check(getSelfInventory(), msg.getHostUuid(), new Completion(msg) {
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
                evt.setError(errorCode);
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

    private void handle(final APIAttachL2NetworkToClusterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("attach-l2-network-to-cluster-%s", msg.getClusterUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                attachL2NetworkToCluster(msg, new NoErrorCompletion(msg,chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
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
                SQL.New(SharedResourceVO.class).eq(SharedResourceVO_.resourceUuid, msg.getL2NetworkUuid()).delete();
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

    private void  attachL2NetworkToCluster(final APIAttachL2NetworkToClusterMsg msg, final NoErrorCompletion completion){
        final APIAttachL2NetworkToClusterEvent evt = new APIAttachL2NetworkToClusterEvent(msg.getId());

        long count = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, msg.getL2NetworkUuid()).count();
        if (count != 0) {
            evt.setInventory((L2NetworkInventory) inventoryMgr.valueOf(self));
            bus.publish(evt);
            completion.done();
            return;
        }
        
        new SQLBatch() {

            @Override
            protected void scripts() {

                String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();

                if (L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE.equals(type)) {
                    List<L2NetworkVO> l2s = SQL.New("select l2" +
                            " from L2NetworkVO l2, L2NetworkClusterRefVO ref" +
                            " where l2.uuid = ref.l2NetworkUuid" +
                            " and ref.clusterUuid = :clusterUuid" +
                            " and type = 'L2NoVlanNetwork'")
                            .param("clusterUuid", msg.getClusterUuid()).list();

                    if (l2s.isEmpty()) {
                        return;
                    }

                    L2NetworkVO tl2 = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).find();
                    for (L2NetworkVO l2 : l2s) {
                        if (l2.getPhysicalInterface().equals(tl2.getPhysicalInterface())) {
                            throw new ApiMessageInterceptionException(argerr("There has been a l2Network[uuid:%s, name:%s] attached to cluster[uuid:%s] that has physical interface[%s]. Failed to attach l2Network[uuid:%s]",
                                    l2.getUuid(), l2.getName(), msg.getClusterUuid(), l2.getPhysicalInterface(), tl2.getUuid()));
                        }
                    }
                } else if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(type)) {
                    List<L2VlanNetworkVO> l2s = SQL.New("select l2" +
                            " from L2VlanNetworkVO l2, L2NetworkClusterRefVO ref" +
                            " where l2.uuid = ref.l2NetworkUuid" +
                            " and ref.clusterUuid = :clusterUuid")
                            .param("clusterUuid", msg.getClusterUuid()).list();
                    if (l2s.isEmpty()) {
                        return;
                    }

                    L2VlanNetworkVO tl2 = Q.New(L2VlanNetworkVO.class).eq(L2VlanNetworkVO_.uuid, msg.getL2NetworkUuid()).find();

                    for (L2VlanNetworkVO vl2 : l2s) {
                        if (vl2.getVlan() == tl2.getVlan() && vl2.getPhysicalInterface().equals(tl2.getPhysicalInterface())) {
                            throw new OperationFailureException(argerr("There has been a L2VlanNetwork[uuid:%s, name:%s] attached to cluster[uuid:%s] that has physical interface[%s], vlan[%s]. Failed to attach L2VlanNetwork[uuid:%s]",
                                    vl2.getUuid(), vl2.getName(), msg.getClusterUuid(), vl2.getPhysicalInterface(), vl2.getVlan(), tl2.getUuid()));
                        }
                    }
                }

            }

        }.execute();

        List<HostVO> hosts = Q.New(HostVO.class).eq(HostVO_.clusterUuid,msg.getClusterUuid())
                .notIn(HostVO_.state,asList(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status,HostStatus.Connected).list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);

        prepareL2NetworkOnHosts(hvinvs, new Completion(msg,completion) {
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
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errf.instantiateErrorCode(L2Errors.ATTACH_ERROR, errorCode));
                bus.publish(evt);
                completion.done();
            }
        });
    }

    @Override
    public void deleteHook() {
    }
}
