package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkGetVniExtensionPoint;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.*;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.PingCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.PingRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterCreatePublicVipFlow;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.*;
import static org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData.ADDITIONAL_PUBLIC_NIC_MASK;
import static org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData.GUEST_NIC_MASK;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouter extends ApplianceVmBase {
    private static final CLogger logger = Utils.getLogger(VirtualRouter.class);

    static {
        allowedOperations.addState(VmInstanceState.Running, APIReconnectVirtualRouterMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, APIUpdateVirtualRouterMsg.class.getName());
        allowedOperations.addState(VmInstanceState.Running, ReconnectVirtualRouterVmMsg.class.getName());
    }

    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    protected VirutalRouterDefaultL3ConfigProxy defaultL3ConfigProxy;

    protected VirtualRouterVmInventory vr;

    public VirtualRouter(ApplianceVmVO vo) {
        super(vo);
    }

    public VirtualRouter(VirtualRouterVmVO vo) {
        super(vo);
        vr = new VirtualRouterVmInventory(vo);
    }

    @Override
    protected VmInstanceInventory getSelfInventory() {
        return VirtualRouterVmInventory.valueOf(getSelf());
    }

    @Override
    protected List<Flow> getPostCreateFlows() {
        return vrMgr.getPostCreateFlows();
    }

    @Override
    protected List<Flow> getPostStartFlows() {
        return vrMgr.getPostStartFlows();
    }

    @Override
    protected List<Flow> getPostStopFlows() {
        return vrMgr.getPostStopFlows();
    }

    @Override
    protected List<Flow> getPostRebootFlows() {
        return vrMgr.getPostRebootFlows();
    }

    @Override
    protected List<Flow> getPostDestroyFlows() {
        return vrMgr.getPostDestroyFlows();
    }

    @Override
    protected List<Flow> getPostMigrateFlows() {
        return vrMgr.getPostMigrateFlows();
    }

    protected FlowChain getReconnectChain() {
        return vrMgr.getReconnectFlowChain();
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectVirtualRouterMsg) {
            handle((APIReconnectVirtualRouterMsg) msg);
        } else if (msg instanceof APIUpdateVirtualRouterMsg) {
            handle((APIUpdateVirtualRouterMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof VirtualRouterAsyncHttpCallMsg) {
            handle((VirtualRouterAsyncHttpCallMsg) msg);
        } else if (msg instanceof ReconnectVirtualRouterVmMsg) {
            handle((ReconnectVirtualRouterVmMsg) msg);
        } else if (msg instanceof PingVirtualRouterVmMsg) {
            handle((PingVirtualRouterVmMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(final PingVirtualRouterVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("ping-virtualrouter-%s", self.getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final PingVirtualRouterVmReply reply = new PingVirtualRouterVmReply();
                if ((VmInstanceState.Running != self.getState() && VmInstanceState.Unknown != self.getState())
                        || ApplianceVmStatus.Connecting == getSelf().getStatus()) {
                    reply.setDoReconnect(false);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                PingCmd cmd = new PingCmd();
                cmd.setUuid(self.getUuid());
                restf.asyncJsonPost(buildUrl(vr.getManagementNic().getIp(), VirtualRouterConstant.VR_PING), cmd, new JsonAsyncRESTCallback<PingRsp>(msg, chain) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setDoReconnect(true);
                        reply.setConnected(false);
                        logger.warn(String.format("failed to ping the virtual router vm[uuid:%s], %s. We will reconnect it soon", self.getUuid(), reply.getError()));
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void success(PingRsp ret) {
                        reply.setDoReconnect(true);
                        if (!ret.isSuccess()) {
                            logger.warn(String.format("failed to ping the virtual router vm[uuid:%s], %s. We will reconnect it soon", self.getUuid(), ret.getError()));
                            reply.setConnected(false);
                        } else {
                            boolean connected = self.getUuid().equals(ret.getUuid());
                            if (!connected) {
                                logger.warn(String.format("a signature lost on the virtual router vm[uuid:%s] changed, it's probably caused by the agent restart. We will issue a reconnect soon", self.getUuid()));
                            } else {
                                connected = ApplianceVmStatus.Connected == getSelf().getStatus();
                            }
                            reply.setConnected(connected);
                            reply.setHaStatus(ret.getHaStatus());
                            if ((ret.getHealthy() != null) && (!ret.getHealthy()) && (ret.getHealthDetail() != null)) {
                                fireServiceUnhealthyCanonicalEvent(inerr("virtual router %s unhealthy, detail %s", getSelf().getUuid(), ret.getHealthDetail()));
                            } else {
                                fireServicehealthyCanonicalEvent();
                            }
                        }
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public Class<PingRsp> getReturnClass() {
                        return PingRsp.class;
                    }
                }, TimeUnit.SECONDS, (long)ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class));
            }

            @Override
            public String getName() {
                return "ping-virtual-router";
            }
        });


    }

    private void handle(final ReconnectVirtualRouterVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final ReconnectVirtualRouterVmReply reply = new ReconnectVirtualRouterVmReply();

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    if (msg.isStatusChange()) {
                        changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                    }
                    reply.setError(allowed);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                reconnect(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("reconnect-virtual-router-%s", self.getUuid());
            }
        });
    }

    protected String buildUrl(String mgmtIp, String path) {
        return vrMgr.buildUrl(mgmtIp, path);
    }

    private void handle(final VirtualRouterAsyncHttpCallMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("%s-commands", syncThreadName);
            }

            @Override
            public void run(final SyncTaskChain chain) {
                refreshVO();

                final VirtualRouterAsyncHttpCallReply reply = new VirtualRouterAsyncHttpCallReply();
                if (msg.isCheckStatus() && getSelf().getState() != VmInstanceState.Running) {
                    throw new OperationFailureException(operr("the virtual router[name:%s, uuid:%s, current state:%s] is not running," +
                                    "and cannot perform required operation. Please retry your operation later once it is running", self.getName(), self.getUuid(), self.getState()));
                }

                if (msg.isCheckStatus() && getSelf().getStatus() != ApplianceVmStatus.Connected) {
                    throw new OperationFailureException(operr("virtual router[uuid:%s] is in status of %s that cannot make http call to %s",
                            self.getUuid(), getSelf().getStatus(), msg.getPath()));
                }

                if (vr.getManagementNic() == null) {
                    throw new OperationFailureException(operr("virtual router[uuid:%s] has no management nic that cannot make http call to %s",
                            self.getUuid(), msg.getPath()));
                }

                restf.asyncJsonPost(buildUrl(vr.getManagementNic().getIp(), msg.getPath()), msg.getCommand(), new JsonAsyncRESTCallback<LinkedHashMap>(msg, chain) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setError(err);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void success(LinkedHashMap ret) {
                        reply.setResponse(ret);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public Class<LinkedHashMap> getReturnClass() {
                        return LinkedHashMap.class;
                    }
                }, TimeUnit.SECONDS, ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class).longValue());
            }

            @Override
            protected int getSyncLevel() {
                return vrMgr.getParallelismDegree(self.getUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
    
    private void handle(final APIUpdateVirtualRouterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIUpdateVirtualRouterEvent evt = new APIUpdateVirtualRouterEvent(msg.getId());

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setError(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                updateVirutalRouter(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VirtualRouterVmVO vrVO = dbf.findByUuid(msg.getVmInstanceUuid(), VirtualRouterVmVO.class);
                        evt.setInventory((VirtualRouterVmInventory.valueOf(vrVO)));
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
            public String getName() {
                return String.format("update-virtual-router-%s", self.getUuid());
            }
        });
    }

    @Transactional
    protected void replaceVirtualRouterDefaultNetwork(String vrUuid, String oldL3Uuid, String newL3Uuud) {
        defaultL3ConfigProxy.detachNetworkService(vrUuid, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK,
                Collections.singletonList(oldL3Uuid));
        defaultL3ConfigProxy.attachNetworkService(vrUuid, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK,
                Collections.singletonList(newL3Uuud));
    }

    private void updateVirutalRouter(APIUpdateVirtualRouterMsg msg, final Completion completion) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(msg.getVmInstanceUuid(), VirtualRouterVmVO.class);
        FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
        fchain.setName(String.format("update-virtual-router-%s", msg.getVmInstanceUuid()));
        fchain.then(new Flow() {
            String __name__ = "update-virtual-router-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                replaceVirtualRouterDefaultNetwork(msg.getVmInstanceUuid(), vrVO.getDefaultRouteL3NetworkUuid(),
                        msg.getDefaultRouteL3NetworkUuid());
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                replaceVirtualRouterDefaultNetwork(msg.getVmInstanceUuid(), msg.getDefaultRouteL3NetworkUuid(),
                        vrVO.getDefaultRouteL3NetworkUuid());
                trigger.rollback();
            }
        }).then(new Flow() {
            String __name__ = "release-old-snat-of-vip";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmNicVO oldNic = null;
                for (VmNicVO nic: vrVO.getVmNics()) {
                    if (nic.getL3NetworkUuid().equals(vrVO.getDefaultRouteL3NetworkUuid())) {
                        oldNic = nic;
                        break;
                    }
                }

                if (oldNic == null) {
                    trigger.next();
                    return;
                }

                String vipIp = oldNic.getIp();
                if (vrVO.getDefaultRouteL3NetworkUuid().equals(vrVO.getManagementNetworkUuid())) {
                    VmNicInventory publicNic = vrMgr.getSnatPubicInventory(VirtualRouterVmInventory.valueOf(vrVO));
                    vipIp = publicNic.getIp();
                }

                VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.ip, vipIp)
                        .eq(VipVO_.l3NetworkUuid, oldNic.getL3NetworkUuid()).find();
                if (vipVO == null) {
                    trigger.next();
                    return;
                }

                data.put("oldVip", vipVO);
                ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                struct.setUseFor(NetworkServiceType.SNAT.toString());
                struct.setServiceUuid(vipVO.getUuid());
                Vip vip = new Vip(vipVO.getUuid());
                vip.setStruct(struct);
                vip.release(new Completion(trigger) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                VipVO vipVO = (VipVO) data.get("oldVip");
                if (vipVO == null) {
                    trigger.rollback();
                    return;
                }

                ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                struct.setUseFor(NetworkServiceType.SNAT.toString());
                struct.setServiceUuid(vipVO.getUuid());
                Vip vip = new Vip(vipVO.getUuid());
                vip.setStruct(struct);
                vip.acquire(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "apply-new-snat-of-vip";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmNicVO newNic = null;
                for (VmNicVO nic: vrVO.getVmNics()) {
                    if (nic.getL3NetworkUuid().equals(msg.getDefaultRouteL3NetworkUuid())) {
                        newNic = nic;
                        break;
                    }
                }

                if (newNic == null) {
                    trigger.fail(argerr("virtual router [uuid:%s] does not has nic in l3 network [uuid:s]", vrVO.getUuid(),
                            msg.getDefaultRouteL3NetworkUuid()));
                    return;
                }

                String vipIp = newNic.getIp();
                if (msg.getDefaultRouteL3NetworkUuid().equals(vrVO.getManagementNetworkUuid())) {
                    VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vrVO);
                    vrInv.setDefaultRouteL3NetworkUuid(msg.getDefaultRouteL3NetworkUuid());
                    VmNicInventory publicNic = vrMgr.getSnatPubicInventory(vrInv);
                    vipIp = publicNic.getIp();
                }

                VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.ip, vipIp)
                        .eq(VipVO_.l3NetworkUuid, newNic.getL3NetworkUuid()).find();
                if (vipVO == null) {
                    trigger.fail(argerr("there is no vip [ip:%s] in l3 network [uuid:%s]", vipIp,
                            msg.getDefaultRouteL3NetworkUuid()));
                    return;
                }

                data.put("newVip", vipVO);
                ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                struct.setUseFor(NetworkServiceType.SNAT.toString());
                struct.setServiceUuid(vipVO.getUuid());
                Vip vip = new Vip(vipVO.getUuid());
                vip.setStruct(struct);
                vip.acquire(new Completion(trigger) {
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

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                VipVO vipVO = (VipVO) data.get("newVip");
                if (vipVO == null) {
                    trigger.rollback();
                    return;
                }

                ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                struct.setUseFor(NetworkServiceType.SNAT.toString());
                struct.setServiceUuid(vipVO.getUuid());
                Vip vip = new Vip(vipVO.getUuid());
                vip.setStruct(struct);
                vip.release(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-virtual-router-backend";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                vrMgr.changeVirutalRouterDefaultL3Network(msg.getVmInstanceUuid(), msg.getDefaultRouteL3NetworkUuid(), vrVO.getDefaultRouteL3NetworkUuid(), new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                Map<String, Object> haData = new HashMap<>();
                haData.put(VirtualRouterHaCallbackInterface.Params.TaskName.toString(), VirtualRouterConstant.VR_CHANGE_DEFAULT_ROUTE_JOB);
                haData.put(VirtualRouterHaCallbackInterface.Params.OriginRouterUuid.toString(), msg.getVmInstanceUuid());
                haData.put(VirtualRouterHaCallbackInterface.Params.Struct.toString(), msg.getDefaultRouteL3NetworkUuid());
                haData.put(VirtualRouterHaCallbackInterface.Params.Struct1.toString(), vrVO.getDefaultRouteL3NetworkUuid());
                haBackend.submitVirutalRouterHaTask(haData, completion);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIReconnectVirtualRouterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIReconnectVirtualRouterEvent evt = new APIReconnectVirtualRouterEvent(msg.getId());

                refreshVO();
                ErrorCode allowed = validateOperationByState(msg, self.getState(), SysErrors.OPERATION_ERROR);
                if (allowed != null) {
                    evt.setError(allowed);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                reconnect(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory((ApplianceVmInventory) getSelfInventory());
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
            public String getName() {
                return String.format("reconnect-virtual-router-%s", self.getUuid());
            }
        });
    }

    private void reconnect(final Completion completion) {
        ApplianceVmStatus oldStatus = getSelf().getStatus();

        FlowChain chain = getReconnectChain();
        chain.setName(String.format("reconnect-virtual-router-%s", self.getUuid()));
        chain.getData().put(VirtualRouterConstant.Param.VR.toString(), vr);
        chain.getData().put(Param.IS_RECONNECT.toString(), Boolean.TRUE.toString());
        chain.getData().put(Params.isReconnect.toString(), Boolean.TRUE.toString());
        chain.getData().put(Params.managementNicIp.toString(), vr.getManagementNic().getIp());
        chain.getData().put(Params.applianceVmUuid.toString(), self.getUuid());

        SimpleQuery<ApplianceVmFirewallRuleVO> q = dbf.createQuery(ApplianceVmFirewallRuleVO.class);
        q.add(ApplianceVmFirewallRuleVO_.applianceVmUuid, Op.EQ, getSelf().getUuid());
        List<ApplianceVmFirewallRuleVO> vos = q.list();
        List<ApplianceVmFirewallRuleInventory> rules = ApplianceVmFirewallRuleInventory.valueOf(vos);
        chain.getData().put(ApplianceVmConstant.Params.applianceVmFirewallRules.toString(), rules);
        chain.insert(new Flow() {
            String __name__ = "change-appliancevm-status-to-connecting";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connecting);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Disconnected);
                fireDisconnectedCanonicalEvent(operr("appliance vm %s reconnect failed",
                        getSelf().getUuid()));
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "change-appliancevm-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeApplianceVmStatus(ApplianceVmStatus.Connected);
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                self = dbf.reload(self);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                if (oldStatus == ApplianceVmStatus.Connected) {
                    fireDisconnectedCanonicalEvent(errCode);
                }

                completion.fail(errCode);
            }
        }).start();
    }

    public class virtualRouterAfterAttachNicFlow extends NoRollbackFlow {
        @Override
        public void run(FlowTrigger trigger, Map data) {
            boolean applyToVirtualRouter = (boolean)data.get(Param.APPLY_TO_VIRTUALROUTER.toString());
            if (!applyToVirtualRouter) {
                trigger.next();
                return;
            }

            VmNicInventory nicInventory = (VmNicInventory) data.get(Param.VR_NIC.toString());
            L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, nicInventory.getL3NetworkUuid()).find();

            VirtualRouterCommands.ConfigureNicCmd cmd = new VirtualRouterCommands.ConfigureNicCmd();
            VirtualRouterCommands.NicInfo info = new VirtualRouterCommands.NicInfo();
            info.setDefaultRoute(false);
            info.setMac(nicInventory.getMac());
            info.setNetmask(nicInventory.getNetmask());
            for (UsedIpInventory ip : nicInventory.getUsedIps()) {
                if (ip.getIpVersion() == IPv6Constants.IPv4) {
                    info.setIp(ip.getIp());
                    info.setGateway(ip.getGateway());
                    info.setNetmask(ip.getNetmask());
                } else {
                    info.setIp6(ip.getIp());
                    info.setGateway6(ip.getGateway());
                    NormalIpRangeVO ipr = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, ip.getIpRangeUuid()).find();
                    info.setPrefixLength(ipr.getPrefixLen());
                    info.setAddressMode(ipr.getAddressMode());
                }
            }

            L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l3NetworkVO.getL2NetworkUuid()).find();
            info.setCategory(l3NetworkVO.getCategory().toString());
            info.setL2type(l2NetworkVO.getType());
            info.setPhysicalInterface(l2NetworkVO.getPhysicalInterface());
            for (L2NetworkGetVniExtensionPoint ext : pluginRgty.getExtensionList(L2NetworkGetVniExtensionPoint.class)) {
                if (ext.getL2NetworkVniType().equals(l2NetworkVO.getType())) {
                    info.setVni(ext.getL2NetworkVni(l2NetworkVO.getUuid(), vr.getHostUuid()));
                }
            }
            info.setMtu(new MtuGetter().getMtu(l3NetworkVO.getUuid()));
            cmd.setNics(Arrays.asList(info));

            VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
            cmsg.setCommand(cmd);
            cmsg.setPath(VirtualRouterConstant.VR_CONFIGURE_NIC_PATH);
            cmsg.setVmInstanceUuid(vr.getUuid());
            cmsg.setCheckStatus(true);
            bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(cmsg, new CloudBusCallBack(trigger) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        trigger.fail(reply.getError());
                        return;
                    }

                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    VirtualRouterCommands.ConfigureNicRsp rsp = re.toResponse(VirtualRouterCommands.ConfigureNicRsp.class);
                    if (rsp.isSuccess()) {
                        logger.debug(String.format("successfully add nic[ip:%s, mac:%s] to virtual router vm[uuid:%s, ip:%s]",
                                info.getIp(), info.getMac(), vr.getUuid(), vr.getManagementNic().getIp()));
                        trigger.next();
                    } else {
                        ErrorCode err = operr("unable to add nic[ip:%s, mac:%s] to virtual router vm[uuid:%s ip:%s], because %s",
                                info.getIp(), info.getMac(), vr.getUuid(), vr.getManagementNic().getIp(), rsp.getError());
                        trigger.fail(err);
                    }
                }
            });
        }
    }

    private class virtualRouterApplyServicesAfterAttachNicFlow implements Flow {
        String __name__ = "virtualRouter-apply-services-afterAttachNic";

        private void virtualRouterApplyServicesAfterAttachNic(Iterator<VirtualRouterAfterAttachNicExtensionPoint> it, VmNicInventory nicInv, Completion completion){
            if (!it.hasNext()) {
                completion.success();
                return;
            }

            VirtualRouterAfterAttachNicExtensionPoint ext = it.next();
            logger.debug(String.format("execute afterAttachNic extension %s", ext.getClass().getSimpleName()));
            ext.afterAttachNic(nicInv, new Completion(completion) {
                @Override
                public void success() {
                    virtualRouterApplyServicesAfterAttachNic(it, nicInv, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        }

        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            boolean applyToVirtualRouter = (boolean)data.get(Param.APPLY_TO_VIRTUALROUTER.toString());
            if (!applyToVirtualRouter) {
                trigger.next();
                return;
            }

            if (nicInv.isIpv6OnlyNic()) {
                trigger.next();
                return;
            }
            
            Iterator<VirtualRouterAfterAttachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterAfterAttachNicExtensionPoint.class).iterator();
            virtualRouterApplyServicesAfterAttachNic(it, nicInv,  new Completion(trigger) {
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

        private void virtualRouterApplyServicesAfterAttachNicRollback(Iterator<VirtualRouterAfterAttachNicExtensionPoint> it, VmNicInventory nicInv, NoErrorCompletion completion){
            if (!it.hasNext()) {
                completion.done();
                return;
            }

            VirtualRouterAfterAttachNicExtensionPoint ext = it.next();
            ext.afterAttachNicRollback(nicInv, new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    virtualRouterApplyServicesAfterAttachNicRollback(it, nicInv, completion);
                }
            });
        }

        @Override
        public void rollback(FlowRollback trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            Iterator<VirtualRouterAfterAttachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterAfterAttachNicExtensionPoint.class).iterator();
            virtualRouterApplyServicesAfterAttachNicRollback(it, nicInv, new NoErrorCompletion() {
                @Override
                public void done() {
                    trigger.rollback();
                }
            });
        }
    }

    @Override
    protected void afterAttachNic(VmNicInventory nicInventory, Completion completion) {
        super.afterAttachNic(nicInventory, true, completion);
    }

    @Override
    protected void afterAttachNic(VmNicInventory nicInventory, boolean applyToBackend, Completion completion) {
        VmNicVO vo = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nicInventory.getUuid()).find();
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vo.getL3NetworkUuid()).find();

        if (l3NetworkVO.getCategory().equals(L3NetworkCategory.Private)) {
            vo.setMetaData(GUEST_NIC_MASK.toString());

            UsedIpVO usedIpVO = Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, nicInventory.getUsedIpUuid()).find();
            usedIpVO.setMetaData(GUEST_NIC_MASK.toString());
            dbf.updateAndRefresh(usedIpVO);
        } else {
            vo.setMetaData(ADDITIONAL_PUBLIC_NIC_MASK.toString());
        }
        vo = dbf.updateAndRefresh(vo);
        logger.debug(String.format("updated metadata of vmnic[uuid: %s]", vo.getUuid()));

        VirtualRouterVmVO vrVo = dbf.findByUuid(self.getUuid(), VirtualRouterVmVO.class);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(Param.VR_NIC.toString(), VmNicInventory.valueOf(vo));
        data.put(Param.SNAT.toString(), Boolean.FALSE);
        data.put(Param.VR.toString(), VirtualRouterVmInventory.valueOf(vrVo));
        data.put(Param.APPLY_TO_VIRTUALROUTER.toString(), applyToBackend);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("apply-services-after-attach-nic-%s-from-virtualrouter-%s", nicInventory.getUuid(), nicInventory.getVmInstanceUuid()));
        chain.setData(data);
        chain.then(new virtualRouterAfterAttachNicFlow());
        chain.then(new VirtualRouterCreatePublicVipFlow());
        chain.then(new virtualRouterApplyServicesAfterAttachNicFlow());
        chain.then(haBackend.getAttachL3NetworkFlow());
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

    private void virtualRouterAfterDetachNic(Iterator<VirtualRouterAfterDetachNicExtensionPoint> exts, VmNicInventory nicInventory, Completion completion) {
        if (!exts.hasNext()) {
            completion.success();
            return;
        }

        VirtualRouterAfterDetachNicExtensionPoint ext = exts.next();
        ext.afterDetachVirtualRouterNic(nicInventory, new Completion(completion) {
            @Override
            public void success() {
                virtualRouterAfterDetachNic(exts, nicInventory, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                virtualRouterAfterDetachNic(exts, nicInventory, completion);
            }
        });
    }

    @Override
    protected void afterDetachNic(VmNicInventory nicInventory, boolean isRollback, Completion completion) {
        if (isRollback) {
            completion.success();
            return;
        }

        List<VirtualRouterAfterDetachNicExtensionPoint> exts = pluginRgty.getExtensionList(VirtualRouterAfterDetachNicExtensionPoint.class);
        virtualRouterAfterDetachNic(exts.iterator(), nicInventory, new Completion(completion) {
            @Override
            public void success() {
                haBackend.detachL3NetworkFromVirtualRouterHaGroup(nicInventory.getVmInstanceUuid(),
                        nicInventory.getL3NetworkUuid(), isRollback, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
    private static class virtualRouterReleaseVipbeforeDetachNic extends NoRollbackFlow {
        @Autowired
        private VipManager vipMgr;
        String __name__ = "virtualRouter-beforeDetachNic";

        private void virtualRouterReleaseVipServices(Iterator<String> it, VipInventory vip, Completion completion) {
            if (!it.hasNext()) {
                completion.success();
                return;
            }
            String service = it.next();
            VipReleaseExtensionPoint ext = vipMgr.getVipReleaseExtensionPoint(service);
            ext.releaseServicesOnVip(vip, new Completion(completion) {
                @Override
                public void success() {
                    virtualRouterReleaseVipServices(it, vip, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    /* even failed, continue release */
                    virtualRouterReleaseVipServices(it, vip, completion);
                }
            });
        }

        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nic = (VmNicInventory) data.get(Param.VR_NIC.toString());
            if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
                trigger.next();
                return;
            }

            if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
                trigger.next();
                return;
            }

            VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf((VirtualRouterVmVO)
                    Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find());

            /*
            * fixme: to be done, the active will not done in standby vpc router because
            * VipPeerL3NetworkRefVO record has been deleted during that of master router
            * this will result the vip delete action will not done in agent before detach nic and
            * not delete some configure such as vip QoS, ifbx.
            */
            List<VipVO> vips = SQL.New("select distinct vip from VipVO vip, VipPeerL3NetworkRefVO ref " +
                    "where ref.vipUuid = vip.uuid and ref.l3NetworkUuid in (:routerNetworks) " +
                    "and vip.l3NetworkUuid = :l3Uuid")
                                  .param("l3Uuid", nic.getL3NetworkUuid())
                                  .param("routerNetworks", vr.getAllL3Networks())
                                  .list();

            if (vips.isEmpty()) {
                trigger.next();
                return;
            }
            ErrorCodeList errList = new ErrorCodeList();
            new While<>(vips).all((vip, completion) -> {
                Set<String> services = vip.getServicesTypes();
                if (services == null || services.isEmpty()) {
                    completion.done();
                    return;
                }
                virtualRouterReleaseVipServices(services.iterator(), VipInventory.valueOf(vip), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errList.getCauses().add(errorCode);
                        completion.done();
                    }
                });
            }).run(new NoErrorCompletion() {
                @Override
                public void done() {
                    if (errList.getCauses().size() > 0) {
                        trigger.fail(errList.getCauses().get(0));
                    } else {
                        trigger.next();
                    }
                }
            });
        }
    }

    public class virtualRouterbeforeDetachNic extends NoRollbackFlow {
        String __name__ = "virtualRouter-beforeDetachNic";
        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nicInventory = (VmNicInventory) data.get(Param.VR_NIC.toString());
            VirtualRouterCommands.RemoveNicCmd cmd = new VirtualRouterCommands.RemoveNicCmd();
            VirtualRouterCommands.NicInfo info = new VirtualRouterCommands.NicInfo();
            info.setIp(nicInventory.getIp());
            info.setDefaultRoute(false);
            info.setGateway(nicInventory.getGateway());
            info.setMac(nicInventory.getMac());
            info.setNetmask(nicInventory.getNetmask());
            cmd.setNics(Arrays.asList(info));

            VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
            cmsg.setCommand(cmd);
            cmsg.setPath(VirtualRouterConstant.VR_REMOVE_NIC_PATH);
            cmsg.setVmInstanceUuid(vr.getUuid());
            cmsg.setCheckStatus(true);
            bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(cmsg, new CloudBusCallBack(trigger) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("detach nic[%s] from virtual router vm[uuid:%s, ip:%s] failed because %s",
                                info, vr.getUuid(), vr.getManagementNic().getIp(), reply.getError().getDetails()));
                        trigger.next();
                        return;
                    }

                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    VirtualRouterCommands.RemoveNicRsp rsp = re.toResponse(VirtualRouterCommands.RemoveNicRsp.class);
                    if (rsp.isSuccess()) {
                        logger.debug(String.format("successfully detach nic[%s] from virtual router vm[uuid:%s, ip:%s]",info, vr.getUuid(), vr.getManagementNic()
                                .getIp()));
                        trigger.next();
                    } else {
                        logger.warn(String.format("unable to detach nic[%s] from virtual router vm[uuid:%s ip:%s], because %s",
                                info, vr.getUuid(), vr.getManagementNic().getIp(), rsp.getError()));
                        trigger.next();;
                    }
                }
            });
        }
    }

    private class virtualRouterReleaseServicesbeforeDetachNicFlow implements Flow {
        String __name__ = "virtualRouter-release-services-before-detach-nic";

        private void virtualRouterReleaseServices(final Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it, VmNicInventory nicInv, Completion completion) {
            if (!it.hasNext()) {
                completion.success();
                return;
            }

            VirtualRouterBeforeDetachNicExtensionPoint ext = it.next();
            logger.debug(String.format("virtual router release service before detach l3 network for %s", ext.getClass().getSimpleName()));
            ext.beforeDetachNic(nicInv, new Completion(completion) {
                @Override
                public void success() {
                    virtualRouterReleaseServices(it, nicInv, completion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    /* even failed, continue the release process */
                    virtualRouterReleaseServices(it, nicInv, completion);
                }
            });
        }

        @Override
        public void run(FlowTrigger trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterBeforeDetachNicExtensionPoint.class).iterator();
            virtualRouterReleaseServices(it, nicInv, new Completion(trigger) {
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

        private void virtualRouterReleaseServicesRollback(final Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it, VmNicInventory nicInv, NoErrorCompletion completion) {
            if (!it.hasNext()) {
                completion.done();
                return;
            }

            VirtualRouterBeforeDetachNicExtensionPoint ext = it.next();
            ext.beforeDetachNicRollback(nicInv, new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    virtualRouterReleaseServicesRollback(it, nicInv, completion);
                }
            });
        }

        @Override
        public void rollback(FlowRollback trigger, Map data) {
            VmNicInventory nicInv = (VmNicInventory) data.get(Param.VR_NIC.toString());
            Iterator<VirtualRouterBeforeDetachNicExtensionPoint> it = pluginRgty.getExtensionList(VirtualRouterBeforeDetachNicExtensionPoint.class).iterator();
            virtualRouterReleaseServicesRollback(it, nicInv, new NoErrorCompletion(trigger) {
                @Override
                public void done() {
                    trigger.rollback();
                }
            });
        }
    }

    @Override
    protected void beforeDetachNic(VmNicInventory nicInventory, Completion completion) {
        Map data = new HashMap();
        data.put(Param.VR_NIC.toString(), nicInventory);
        data.put(Param.VR.toString(), vr);
        ApplianceVmVO appvm = Q.New(ApplianceVmVO.class)
                .eq(ApplianceVmVO_.uuid, nicInventory.getVmInstanceUuid()).find();
        if (appvm.getStatus().equals(ApplianceVmStatus.Disconnected)) {
            logger.debug(String.format("appliance vm[uuid: %s] current status is [%s], skip before detach nic",
                    appvm.getUuid(), appvm.getStatus()));
            completion.success();
            return;
        }

        if (appvm.getState().equals(VmInstanceState.Stopped)) {
            logger.debug(String.format("appliance vm[uuid: %s] current state is [%s], skip before detach nic",
                    appvm.getUuid(), appvm.getStatus()));
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("release-services-before-detach-nic-%s-from-virtualrouter-%s", nicInventory.getUuid(), nicInventory.getVmInstanceUuid()));
        chain.setData(data);
        chain.insert(new virtualRouterReleaseServicesbeforeDetachNicFlow());
        chain.then(new virtualRouterReleaseVipbeforeDetachNic());
        chain.then(new virtualRouterbeforeDetachNic());
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

}
