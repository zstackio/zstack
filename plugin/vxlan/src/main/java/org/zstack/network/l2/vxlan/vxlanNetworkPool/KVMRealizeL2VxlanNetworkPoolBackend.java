package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.network.l2.vxlan.vtep.CreateVtepMsg;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkGlobalConfig;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by weiwang on 20/03/2017.
 */
public class KVMRealizeL2VxlanNetworkPoolBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint {
    private static CLogger logger = Utils.getLogger(KVMRealizeL2VxlanNetworkPoolBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private CloudBus bus;

    private static String VTEP_IP = "vtepIp";
    private static String NEED_POPULATE = "needPopulate";

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        completion.success();
    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        check(l2Network, hostUuid, true, completion);
    }

    public void check(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        final L2VxlanNetworkPoolInventory vxlanPool = (L2VxlanNetworkPoolInventory) l2Network;
        final String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("check-l2-vxlan-pool-%s-on-host-%s", l2Network.getUuid(), hostUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("check-vtep-ip-for-l2-vxlan-pool-%s", l2Network.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VxlanKvmAgentCommands.CheckVxlanCidrCmd cmd = new VxlanKvmAgentCommands.CheckVxlanCidrCmd();
                cmd.setCidr(getAttachedCidrs(vxlanPool.getUuid()).get(clusterUuid));
                if (!l2Network.getPhysicalInterface().isEmpty()) {
                    cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
                }
                VtepVO vtep = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, vxlanPool.getUuid()).eq(VtepVO_.hostUuid, hostUuid).find();
                if (vtep != null) {
                    cmd.setVtepip(vtep.getVtepIp());
                }

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setCommand(cmd);
                msg.setPath(VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH);
                msg.setNoStatusCheck(noStatusCheck);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply hreply = reply.castReply();
                        VxlanKvmAgentCommands.CheckVxlanCidrResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CheckVxlanCidrResponse.class);
                        if (!rsp.isSuccess()) {
                            ErrorCode err = operr("failed to check cidr[%s] for l2VxlanNetworkPool[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                                    cmd.getCidr(), vxlanPool.getUuid(), vxlanPool.getName(), hostUuid, rsp.getError());
                            completion.fail(err);
                            return;
                        }

                        data.put(VTEP_IP, rsp.getVtepIp());
                        String info = String.format("successfully checked cidr[%s] for l2VxlanNetworkPool[uuid:%s, name:%s] on kvm host[uuid:%s]",
                                cmd.getCidr(), vxlanPool.getUuid(), vxlanPool.getName(), hostUuid);
                        logger.debug(info);
                        trigger.next();
                    }
                });
            }

        }).then(new NoRollbackFlow() {
            String __name__ = String.format("create-vtep-for-l2-vxlan-pool-%s", l2Network.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                data.put(NEED_POPULATE, false);

                List<VtepVO> vtepVOS = Q.New(VtepVO.class)
                        .eq(VtepVO_.poolUuid, l2Network.getUuid())
                        .eq(VtepVO_.hostUuid, hostUuid)
                        .list();

                if (vtepVOS == null || vtepVOS.isEmpty()) {
                    trigger.next();
                    return;
                } else if (vtepVOS.size() > 1) {
                    throw new CloudRuntimeException(String.format("multiple vteps[ips: %s] found on host[uuid: %s]",
                            vtepVOS.stream().map(v -> v.getVtepIp()).collect(Collectors.toSet()), hostUuid));

                } else if (vtepVOS.get(0).getVtepIp().equals(data.get(VTEP_IP))) {
                    logger.debug(String.format(
                            "vtep[ip:%s] from host[uuid:%s] for l2 vxlan network pool[uuid:%s] checks successfully",
                            vtepVOS.get(0).getVtepIp(), hostUuid, l2Network.getUuid()));
                    trigger.next();
                    return;
                } else {
                    logger.debug(String.format(
                            "remove deprecated vtep[ip:%s] from host[uuid:%s] for l2 vxlan network pool[uuid:%s]",
                            vtepVOS.get(0).getVtepIp(), hostUuid, l2Network.getUuid()));
                    dbf.remove(vtepVOS.get(0));
                }

                logger.debug(String.format(
                        "creating vtep[ip:%s] on host[uuid:%s] for l2 vxlan network pool[uuid:%s]",
                        data.get(VTEP_IP), hostUuid, l2Network.getUuid()));

                CreateVtepMsg cmsg = new CreateVtepMsg();
                cmsg.setPoolUuid(l2Network.getUuid());
                cmsg.setClusterUuid(clusterUuid);
                cmsg.setHostUuid(hostUuid);
                cmsg.setPort(VXLAN_PORT);
                cmsg.setVtepIp((String) data.get(VTEP_IP));
                cmsg.setType(KVM_VXLAN_TYPE);

                bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2Network.getUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                            trigger.fail(reply.getError());
                            return;
                        }
                        logger.debug(String.format("created new vtep [%s] on vxlan network pool [%s]", cmsg.getVtepIp(), l2Network.getUuid()));
                        data.put(NEED_POPULATE, true);
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("realize-vtep-for-l2-vxlan-pool-%s", l2Network.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (data.get(NEED_POPULATE).equals(false) && VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(Boolean.class)) {
                    logger.debug("no need populate for vxlan networks");
                    trigger.next();
                    return;
                }
                List<VxlanNetworkVO> vxlanNetworkVOS;
                if (VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(Boolean.class)) {
                    vxlanNetworkVOS = SQL.New("select vxlan from VxlanNetworkVO vxlan, VmNicVO nic, L3NetworkVO l3 " +
                            "where vxlan.uuid = l3.l2NetworkUuid " +
                            "and nic.l3NetworkUuid = l3.uuid " +
                            "and vxlan.poolUuid = :poolUuid " +
                            "group by vxlan.uuid")
                                         .param("poolUuid", l2Network.getUuid())
                                         .list();
                } else {
                    vxlanNetworkVOS = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.poolUuid, l2Network.getUuid()).list();
                }

                if (vxlanNetworkVOS == null || vxlanNetworkVOS.isEmpty()) {
                    trigger.next();
                    return;
                }

                final List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2Network.getUuid()).list();

                if (vteps == null || vteps.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<Integer> vnis = vxlanNetworkVOS.stream()
                        .map(v -> v.getVni())
                        .collect(Collectors.toList());

                final VxlanKvmAgentCommands.CreateVxlanBridgesCmd cmd = new VxlanKvmAgentCommands.CreateVxlanBridgesCmd();
                cmd.setVtepIp((String) data.get(VTEP_IP));
                cmd.setVnis(vnis);
                cmd.setL2NetworkUuid(l2Network.getUuid());
                cmd.setPeers(vteps.stream()
                        .map(v -> v.getVtepIp())
                        .filter(v -> !v.equals(cmd.getVtepIp()))
                        .collect(Collectors.toList()));

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setCommand(cmd);
                msg.setNoStatusCheck(noStatusCheck);
                msg.setPath(VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply hreply = reply.castReply();
                        VxlanKvmAgentCommands.CreateVxlanBridgeResponse rsp = hreply.toResponse(VxlanKvmAgentCommands.CreateVxlanBridgeResponse.class);
                        if (!rsp.isSuccess()) {
                            ErrorCode err = operr("failed to realize vxlan network pool[uuid:%s, type:%s, vnis:%s] on kvm host[uuid:%s], because %s",
                                    l2Network.getUuid(), l2Network.getType(), vnis, hostUuid, rsp.getError());
                            trigger.fail(err);
                            return;
                        }

                        String info = String.format(
                                "successfully realize vxlan network pool[uuid:%s, type:%s, vnis:%s] on kvm host[uuid:%s]",
                                l2Network.getUuid(), l2Network.getType(), vnis, hostUuid);
                        logger.debug(info);

                        for (VxlanNetworkVO vo : vxlanNetworkVOS) {
                            SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(vo.getUuid());
                            creator.inherent = true;
                            creator.ignoreIfExisting = true;
                            creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN,
                                    KVMRealizeL2VxlanNetworkBackend.makeBridgeName(vo.getVni()))));
                            creator.create();
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("populate-vtep-for-l2-vxlan-pool-%s", l2Network.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (data.get(NEED_POPULATE).equals(false)) {
                    trigger.next();
                    return;
                }

                List<VtepVO> vteps = Q.New(VtepVO.class).eq(VtepVO_.poolUuid, l2Network.getUuid()).list();

                if (vteps == null || vteps.size() <= 1) {
                    logger.debug("no need to populate fdb since there are only one vtep or less");
                    trigger.next();
                    return;
                }

                List<String> vxlanNetworkUuids = Q.New(VxlanNetworkVO.class)
                        .select(VxlanNetworkVO_.uuid)
                        .eq(VxlanNetworkVO_.poolUuid, l2Network.getUuid())
                        .listValues();

                new While<>(vteps).all((vtep, completion1) -> {
                    Set<String> peers = vteps.stream()
                            .map(v -> v.getVtepIp())
                            .collect(Collectors.toSet());
                    peers.remove(vtep.getVtepIp());

                    logger.info(String.format("populate fdb to vtep[ip:%s] for vxlan network pool %s with vxlan network[uuids:%s] to host[uuid:%s]",
                            vtep.getVtepIp(), l2Network.getUuid(), vxlanNetworkUuids, vtep.getHostUuid()));

                    VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd();
                    cmd.setPeers(new ArrayList<>(peers));
                    cmd.setNetworkUuids(vxlanNetworkUuids);

                    KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                    msg.setHostUuid(vtep.getHostUuid());
                    msg.setCommand(cmd);
                    msg.setPath(VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH);
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
        return L2NetworkType.valueOf(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf("KVM");
    }

    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return getSupportedL2NetworkType();
    }

    @Override
    public KVMAgentCommands.NicTO completeNicInformation(L2NetworkInventory l2Network, VmNicInventory nic) {
        VxlanNetworkPoolVO vo = dbf.findByUuid(l2Network.getUuid(), VxlanNetworkPoolVO.class);
        KVMAgentCommands.NicTO to = new KVMAgentCommands.NicTO();
        to.setMac(nic.getMac());
        to.setUuid(nic.getUuid());
        to.setDeviceId(nic.getDeviceId());
        to.setNicInternalName(nic.getInternalName());
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
}
