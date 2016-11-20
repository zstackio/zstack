package org.zstack.network.service.virtualrouter.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipManager;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentResponse;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipBackend;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/9/2015.
 */
public class VirtualRouterLoadBalancerBackend extends AbstractVirtualRouterBackend implements LoadBalancerBackend {
    private static CLogger logger = Utils.getLogger(VirtualRouterLoadBalancerBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    private VirtualRouterVipBackend vipVrBkd;
    @Autowired
    private VipManager vipMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findVirtualRouterVm(String lbUuid) {
        String sql = "select vr from VirtualRouterVmVO vr, VirtualRouterLoadBalancerRefVO ref where ref.virtualRouterVmUuid =" +
                " vr.uuid and ref.loadBalancerUuid = :lbUuid";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("lbUuid", lbUuid);
        List<VirtualRouterVmVO> vrs = q.getResultList();
        return vrs.isEmpty() ? null : VirtualRouterVmInventory.valueOf(vrs.get(0));
    }

    public static class LbTO {
        String lbUuid;
        String listenerUuid;
        String vip;
        List<String> nicIps;
        int instancePort;
        int loadBalancerPort;
        String mode;
        List<String> parameters;

        public String getListenerUuid() {
            return listenerUuid;
        }

        public void setListenerUuid(String listenerUuid) {
            this.listenerUuid = listenerUuid;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public void setParameters(List<String> parameters) {
            this.parameters = parameters;
        }

        public String getLbUuid() {
            return lbUuid;
        }

        public void setLbUuid(String lbUuid) {
            this.lbUuid = lbUuid;
        }

        public String getVip() {
            return vip;
        }

        public void setVip(String vip) {
            this.vip = vip;
        }

        public List<String> getNicIps() {
            return nicIps;
        }

        public void setNicIps(List<String> nicIps) {
            this.nicIps = nicIps;
        }

        public int getInstancePort() {
            return instancePort;
        }

        public void setInstancePort(int instancePort) {
            this.instancePort = instancePort;
        }

        public int getLoadBalancerPort() {
            return loadBalancerPort;
        }

        public void setLoadBalancerPort(int loadBalancerPort) {
            this.loadBalancerPort = loadBalancerPort;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class RefreshLbCmd extends AgentCommand {
        List<LbTO> lbs;

        public List<LbTO> getLbs() {
            return lbs;
        }

        public void setLbs(List<LbTO> lbs) {
            this.lbs = lbs;
        }
    }

    public static class RefreshLbRsp extends AgentResponse {
    }

    public static class DeleteLbCmd extends AgentCommand {
        List<LbTO> lbs;

        public List<LbTO> getLbs() {
            return lbs;
        }

        public void setLbs(List<LbTO> lbs) {
            this.lbs = lbs;
        }
    }

    public static class DeleteLbRsp extends AgentResponse {
    }

    public static final String REFRESH_LB_PATH = "/lb/refresh";
    public static final String DELETE_LB_PATH = "/lb/delete";

    private List<LbTO> makeLbTOs(final LoadBalancerStruct struct) {
        SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
        q.select(VipVO_.ip);
        q.add(VipVO_.uuid, Op.EQ, struct.getLb().getVipUuid());
        final String vip = q.findValue();

        return CollectionUtils.transformToList(struct.getListeners(), new Function<LbTO, LoadBalancerListenerInventory>() {
            @Override
            public LbTO call(LoadBalancerListenerInventory l) {
                LbTO to = new LbTO();
                to.setInstancePort(l.getInstancePort());
                to.setLoadBalancerPort(l.getLoadBalancerPort());
                to.setLbUuid(l.getLoadBalancerUuid());
                to.setListenerUuid(l.getUuid());
                to.setMode(l.getProtocol());
                to.setVip(vip);
                to.setNicIps(CollectionUtils.transformToList(l.getVmNicRefs(), new Function<String, LoadBalancerListenerVmNicRefInventory>() {
                    @Override
                    public String call(LoadBalancerListenerVmNicRefInventory arg) {
                        if (LoadBalancerVmNicStatus.Active.toString().equals(arg.getStatus()) || LoadBalancerVmNicStatus.Pending.toString().equals(arg.getStatus())) {
                            VmNicInventory nic = struct.getVmNics().get(arg.getVmNicUuid());
                            if (nic == null) {
                                throw new CloudRuntimeException(String.format("cannot find nic[uuid:%s]", arg.getVmNicUuid()));
                            }
                            return nic.getIp();
                        }
                        return null;
                    }
                }));

                SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
                q.select(SystemTagVO_.tag);
                q.add(SystemTagVO_.resourceUuid, Op.EQ, l.getUuid());
                q.add(SystemTagVO_.resourceType, Op.EQ, LoadBalancerListenerVO.class.getSimpleName());
                List<String> tags = q.listValue();
                to.setParameters(tags);
                return to;
            }
        });
    }

    private void refresh(VirtualRouterVmInventory vr, LoadBalancerStruct struct, final Completion completion) {
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(REFRESH_LB_PATH);

        RefreshLbCmd cmd = new RefreshLbCmd();
        cmd.lbs = makeLbTOs(struct);

        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "5m"));
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    RefreshLbRsp rsp = ((VirtualRouterAsyncHttpCallReply) reply).toResponse(RefreshLbRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(errf.stringToOperationError(rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void startVrIfNeededAndRefresh(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, final Completion completion) {
        if (!VmInstanceState.Stopped.toString().equals(vr.getState())) {
            refresh(vr, struct, completion);
            return;
        }

        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("start-vr-%s-and-refresh-lb-%s", vr.getUuid(), struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "start-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        StartVmInstanceMsg msg = new StartVmInstanceMsg();
                        msg.setVmInstanceUuid(vr.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
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

                flow(new Flow() {
                    String __name__ = "create-vip-on-vr";
                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SimpleQuery<VirtualRouterVipVO> q = dbf.createQuery(VirtualRouterVipVO.class);
                        q.add(VirtualRouterVipVO_.uuid, Op.EQ, vip.getUuid());
                        q.add(VirtualRouterVipVO_.virtualRouterVmUuid, Op.EQ, vr.getUuid());
                        if (q.isExists()) {
                            trigger.next();
                        } else {
                            vipVrBkd.acquireVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                                @Override
                                public void success() {
                                    vipMgr.saveVipInfo(struct.getLb().getVipUuid(), VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE, vr.getGuestNic().getL3NetworkUuid());
                                    success = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (success) {
                            vipVrBkd.releaseVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });
                        } else {
                            trigger.rollback();
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        refresh(vr, struct, new Completion(trigger) {
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
    public void addVmNics(final LoadBalancerStruct struct, List<VmNicInventory> nics, final Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr != null) {
            startVrIfNeededAndRefresh(vr, struct, completion);
            return;
        }

        VmNicInventory nic = nics.get(0);
        final L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));

        final boolean separateVr = LoadBalancerSystemTags.SEPARATE_VR.hasTag(struct.getLb().getUuid());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-nic-to-vr-lb-%s", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            VirtualRouterVmInventory vr;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "lock-vip";

                    boolean success = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vipMgr.lockVip(vip, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
                        success = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            vipMgr.unlockVip(vip);
                        }
                        trigger.rollback();
                    }
                });

                if (separateVr) {
                    flow(new Flow() {
                        String __name__ = "create-separate-vr";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            VirtualRouterStruct s = new VirtualRouterStruct();
                            s.setInherentSystemTags(list(VirtualRouterSystemTags.DEDICATED_ROLE_VR.getTagFormat(), VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat()));
                            s.setVirtualRouterVmSelector(new VirtualRouterVmSelector() {
                                @Override
                                public VirtualRouterVmVO select(List<VirtualRouterVmVO> vrs) {
                                    return null;
                                }
                            });
                            s.setL3Network(l3);
                            s.setNotGatewayForGuestL3Network(true);

                            acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger) {
                                @Override
                                public void success(VirtualRouterVmInventory returnValue) {
                                    vr = returnValue;
                                    new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (vr == null) {
                                trigger.rollback();
                                return;
                            }

                            DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        //TODO:
                                        logger.warn(String.format("failed to destroy vr[uuid:%s], %s. Need a cleanup", vr.getUuid(), reply.getError()));
                                    }

                                    trigger.rollback();
                                }
                            });
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "acquire-vr";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            VirtualRouterStruct s = new VirtualRouterStruct();
                            s.setL3Network(l3);

                            acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger) {
                                @Override
                                public void success(VirtualRouterVmInventory returnValue) {
                                    vr = returnValue;
                                    new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
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

                flow(new Flow() {
                    String __name__ = "create-vip-on-vr";
                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        vipVrBkd.acquireVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                            @Override
                            public void success() {
                                vipMgr.saveVipInfo(struct.getLb().getVipUuid(), VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE, vr.getGuestNic().getL3NetworkUuid());
                                success = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (success) {
                            vipVrBkd.releaseVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });
                        } else {
                            trigger.rollback();
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-on-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        refresh(vr, struct, new Completion(trigger) {
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
                        VirtualRouterLoadBalancerRefVO ref = new VirtualRouterLoadBalancerRefVO();
                        ref.setLoadBalancerUuid(struct.getLb().getUuid());
                        ref.setVirtualRouterVmUuid(vr.getUuid());
                        dbf.persist(ref);

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
    public void addVmNic(final LoadBalancerStruct struct, VmNicInventory nic, final Completion completion) {
        addVmNics(struct, list(nic), completion);
    }

    @Override
    public void removeVmNic(LoadBalancerStruct struct, VmNicInventory nic, Completion completion) {
        removeVmNics(struct, list(nic), completion);
    }

    @Override
    public void removeVmNics(LoadBalancerStruct struct, List<VmNicInventory> nic, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
            // no need to remove as the vr is stopped
            completion.success();
            return;
        }

        refresh(vr, struct, completion);
    }

    @Override
    public void addListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find virtual router for load balancer [uuid:%s]", struct.getLb().getUuid())
            ));
        }

        startVrIfNeededAndRefresh(vr, struct, completion);
    }

    @Override
    public void removeListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
            completion.success();
            return;
        }

        refresh(vr, struct, completion);
    }

    @Override
    public void destroyLoadBalancer(final LoadBalancerStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-lb-%s-from-vr", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-from-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
                        if (vr == null) {
                            // the vr has been destroyed
                            trigger.next();
                            return;
                        }

                        SimpleQuery<VirtualRouterLoadBalancerRefVO> q = dbf.createQuery(VirtualRouterLoadBalancerRefVO.class);
                        q.add(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid, Op.EQ, struct.getLb().getUuid());
                        q.add(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, Op.EQ, vr.getUuid());
                        final VirtualRouterLoadBalancerRefVO ref = q.find();

                        List<String> roles = new VirtualRouterRoleManager().getAllRoles(vr.getUuid());
                        if (roles.size() == 1 && roles.contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat())) {
                            DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        dbf.remove(ref);
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        } else if (roles.size() > 1 && roles.contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat())) {
                            DeleteLbCmd cmd = new DeleteLbCmd();
                            cmd.setLbs(makeLbTOs(struct));

                            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            msg.setPath(DELETE_LB_PATH);
                            msg.setCommand(cmd);
                            msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "5m"));
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        DeleteLbRsp rsp = ((VirtualRouterAsyncHttpCallReply)reply).toResponse(DeleteLbRsp.class);
                                        if (rsp.isSuccess()) {
                                            dbf.remove(ref);
                                            trigger.next();
                                        } else {
                                            trigger.fail(errf.stringToOperationError(rsp.getError()));
                                        }
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        } else {
                            throw new CloudRuntimeException(String.format("wrong virtual router roles%s. it doesn't have the role[%s]",
                                    roles, VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat()));
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "unlock-vip";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));
                        vipMgr.releaseAndUnlockVip(vip, true, new Completion(trigger) {
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
    public void refresh(LoadBalancerStruct struct, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        startVrIfNeededAndRefresh(vr, struct, completion);
    }

    void syncOnStart(VirtualRouterVmInventory vr, List<LoadBalancerStruct> structs, final Completion completion) {
        List<LbTO> tos = new ArrayList<LbTO>();
        for (LoadBalancerStruct s : structs) {
            tos.addAll(makeLbTOs(s));
        }

        RefreshLbCmd cmd = new RefreshLbCmd();
        cmd.lbs = tos;

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(REFRESH_LB_PATH);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCheckStatus(false);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    VirtualRouterAsyncHttpCallReply kr = reply.castReply();
                    RefreshLbRsp rsp = kr.toResponse(RefreshLbRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(errf.stringToOperationError(rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }
}
