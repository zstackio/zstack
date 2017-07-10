package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.InstantiateResourceOnAttachingNicExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.network.l2.vxlan.vtep.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by weiwang on 17/04/2017.
 */
public class KVMRealizeL2VxlanNetworkBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint, InstantiateResourceOnAttachingNicExtensionPoint {
    private static CLogger logger = Utils.getLogger(KVMRealizeL2VxlanNetworkBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private static String vtepIp = "vtepIp";
    private static String needPopulate = "needPopulate";

    private String makeBridgeName(int vxlan) {
        return String.format("br_vx_%s",vxlan);
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final L2VxlanNetworkInventory l2vxlan = (L2VxlanNetworkInventory) l2Network;
        final String vtepIp = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.hostUuid, hostUuid).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).findValue();
        List<String> peers = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).listValues();
        Set<String> p = new HashSet<String>(peers);
        p.remove(vtepIp);
        peers.clear();
        peers.addAll(p);

        String info = String.format(
                "get vtep peers [%s] and vtep ip [%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s]", peers,
                vtepIp, l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid);
        logger.debug(info);

        final VxlanKvmAgentCommands.CreateVxlanBridgeCmd cmd = new VxlanKvmAgentCommands.CreateVxlanBridgeCmd();
        cmd.setVtepIp(vtepIp);
        cmd.setBridgeName(makeBridgeName(l2vxlan.getVni()));
        cmd.setVni(l2vxlan.getVni());
        cmd.setPeers(peers);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setNoStatusCheck(noStatusCheck);
        msg.setPath(VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                VxlanKvmAgentCommands.CreateVxlanBridgeResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CreateVxlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to create bridge[%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s], because %s",
                            cmd.getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format(
                        "successfully realize bridge[%s] for l2Network[uuid:%s, type:%s, vni:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2vxlan.getVni(), hostUuid);
                logger.debug(info);

                SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(l2Network.getUuid());
                creator.inherent = true;
                creator.ignoreIfExisting = true;
                creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, cmd.getBridgeName())));
                creator.create();

                completion.success();
            }
        });
    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        check(l2Network, hostUuid, false, completion);
    }

    public void check(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        final L2VxlanNetworkInventory l2vxlan = (L2VxlanNetworkInventory) l2Network;
        final String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();
        final VxlanNetworkPoolVO poolVO = Q.New(VxlanNetworkPoolVO.class).eq(VxlanNetworkPoolVO_.uuid, l2vxlan.getPoolUuid()).find();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("check-l2-vxlan-%s-on-host-%s", l2Network.getUuid(), hostUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VxlanKvmAgentCommands.CheckVxlanCidrCmd cmd = new VxlanKvmAgentCommands.CheckVxlanCidrCmd();
                cmd.setCidr(getAttachedCidrs(l2vxlan.getPoolUuid()).get(clusterUuid));
                if (!poolVO.getPhysicalInterface().isEmpty()) {
                    cmd.setPhysicalInterfaceName(poolVO.getPhysicalInterface());
                }

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setCommand(cmd);
                msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                msg.setPath(VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH);
                msg.setNoStatusCheck(noStatusCheck);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply hreply = reply.castReply();
                        VxlanKvmAgentCommands.CheckVxlanCidrResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CheckVxlanCidrResponse.class);
                        if (!rsp.isSuccess()) {
                            ErrorCode err = operr("failed to check cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                                    cmd.getCidr(), l2vxlan.getUuid(), l2vxlan.getName(), hostUuid, rsp.getError());
                            trigger.fail(err);
                            return;
                        }

                        String info = String.format("successfully checked cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s]",
                                cmd.getCidr(), l2vxlan.getUuid(), l2vxlan.getName(), hostUuid);
                        logger.debug(info);
                        data.put(vtepIp, rsp.getVtepIp());

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> vtepIps = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).listValues();
                if (vtepIps.contains(data.get(vtepIp))) {
                    data.put(needPopulate, false);
                    trigger.next();
                    return;
                } else {
                    data.put(needPopulate, true);
                }

                CreateVtepMsg cmsg = new CreateVtepMsg();
                cmsg.setPoolUuid(l2vxlan.getPoolUuid());
                cmsg.setClusterUuid(clusterUuid);
                cmsg.setHostUuid(hostUuid);
                cmsg.setPort(VXLAN_PORT);
                cmsg.setVtepIp((String) data.get(vtepIp));
                cmsg.setType(KVM_VXLAN_TYPE);

                bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2vxlan.getPoolUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                            trigger.fail(reply.getError());
                            return;
                        }
                        logger.debug(String.format("created new vtep [%s] on vxlan network pool [%s]", cmsg.getVtepIp(), ((L2VxlanNetworkInventory) l2Network).getPoolUuid()));
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (data.get(needPopulate).equals(false)) {
                    trigger.next();
                    return;
                }

                List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).list();

                if (vteps.size() == 1) {
                    logger.debug("no need to populate fdb since there are only one vtep");
                    trigger.next();
                    return;
                }

                new While<>(vteps).all((vtep, completion1) -> {
                    List<String> peers = new ArrayList<>();
                    for (VtepVO vo : vteps) {
                        if (peers.contains(vo.getVtepIp()) || vo.getVtepIp().equals(vtep.getVtepIp())) {
                            continue;
                        } else {
                            peers.add(vo.getVtepIp());
                        }
                    }

                    logger.info(String.format("populate fdb for vtep %s in vxlan network %s", vtep.getVtepIp(), l2vxlan.getUuid()));

                    VxlanKvmAgentCommands.PopulateVxlanFdbCmd cmd = new VxlanKvmAgentCommands.PopulateVxlanFdbCmd();
                    cmd.setPeers(peers);
                    cmd.setVni(l2vxlan.getVni());

                    KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                    msg.setHostUuid(vtep.getHostUuid());
                    msg.setCommand(cmd);
                    msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                    msg.setPath(VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH);
                    msg.setNoStatusCheck(noStatusCheck);
                    bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                    bus.send(msg, new CloudBusCallBack(completion1) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
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

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(VxlanNetworkConstant.VXLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return getSupportedL2NetworkType();
    }

    @Override
    public KVMAgentCommands.NicTO completeNicInformation(L2NetworkInventory l2Network, VmNicInventory nic) {
        VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), VxlanNetworkVO.class);
        KVMAgentCommands.NicTO to = new KVMAgentCommands.NicTO();
        to.setMac(nic.getMac());
        to.setUuid(nic.getUuid());
        to.setBridgeName(makeBridgeName(vo.getVni()));
        to.setDeviceId(nic.getDeviceId());
        to.setNicInternalName(nic.getInternalName());
        to.setMetaData(String.valueOf(vo.getVni()));
        return to;
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

    @Override
    public void instantiateResourceOnAttachingNic(VmInstanceSpec spec, L3NetworkInventory l3, Completion completion) {
        L2NetworkVO vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l3.getL2NetworkUuid()).find();
        if (!vo.getType().equals(VxlanNetworkConstant.VXLAN_NETWORK_TYPE)) {
            completion.success();
            return;
        } else {
            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            L2VxlanNetworkInventory l2 = L2VxlanNetworkInventory.valueOf((VxlanNetworkVO) Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.uuid, vo.getUuid()).find());
            chain.setName(String.format("attach-l2-vxlan-%s-on-host-%s", l2.getUuid(), spec.getDestHost().getUuid()));
            chain.then(new NoRollbackFlow() {
                           @Override
                           public void run(FlowTrigger trigger, Map data) {
                               check(l2, spec.getDestHost().getUuid(), new Completion(trigger) {
                                   @Override
                                   public void success() {
                                       trigger.next();
                                   }

                                   @Override
                                   public void fail(ErrorCode errorCode) {
                                       logger.debug(String.format("check l2 vxlan failed for %s", errorCode.toString()));
                                       trigger.fail(errorCode);
                                   }
                               });
                           }
                       }).then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    realize(l2, spec.getDestHost().getUuid(), new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.debug(String.format("realize l2 vxlan failed for %s", errorCode.toString()));
                            trigger.fail(errorCode);
                        }
                    });
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
    }

    @Override
    public void releaseResourceOnAttachingNic(VmInstanceSpec spec, L3NetworkInventory l3, NoErrorCompletion completion) {

    }
}
