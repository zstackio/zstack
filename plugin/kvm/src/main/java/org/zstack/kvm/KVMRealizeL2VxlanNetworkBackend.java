package org.zstack.kvm;

import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
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
import org.zstack.network.l2.vxlan.vtep.CreateVtepMsg;
import org.zstack.network.l2.vxlan.vtep.VtepInventory;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KVMConstant.KVM_VXLAN_TYPE;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.VXLAN_PORT;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by weiwang on 17/04/2017.
 */
public class KVMRealizeL2VxlanNetworkBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint {
    private static CLogger logger = Utils.getLogger(KVMRealizeL2VxlanNetworkBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private String makeBridgeName(int vxlan) {
        return String.format("br_vxlan_%s",vxlan);
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final L2VxlanNetworkInventory l2vxlan = (L2VxlanNetworkInventory) l2Network;
        final String vtepIp = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.hostUuid, hostUuid).findValue();
        List<String> peers = Q.New(VtepVO.class).select(VtepVO_.vtepIp).eq(VtepVO_.poolUuid, l2vxlan.getPoolUuid()).listValues();
        peers.remove(vtepIp);

        final KVMAgentCommands.CreateVxlanBridgeCmd cmd = new KVMAgentCommands.CreateVxlanBridgeCmd();
        cmd.setVtepIp(vtepIp);
        cmd.setBridgeName(makeBridgeName(l2vxlan.getVni()));
        cmd.setVni(l2vxlan.getVni());
        cmd.setPeers(peers);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setNoStatusCheck(noStatusCheck);
        msg.setPath(KVMConstant.KVM_REALIZE_L2VXLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.CreateVxlanBridgeResponse rsp = hreply.toResponse(KVMAgentCommands.CreateVxlanBridgeResponse.class);
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
        final KVMAgentCommands.CheckVxlanCidrCmd cmd = new KVMAgentCommands.CheckVxlanCidrCmd();
        final String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();
        cmd.setCidr(getAttachedCidrs(l2vxlan.getPoolUuid()).get(clusterUuid));
        if (!l2Network.getPhysicalInterface().equals("No use")) {
            cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        }

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(KVMConstant.KVM_CHECK_L2VXLAN_NETWORK_PATH);
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
                KVMAgentCommands.CheckVxlanCidrResponse rsp = hreply.toResponse(KVMAgentCommands.CheckVxlanCidrResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to check cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                            cmd.getCidr(), l2vxlan.getUuid(), l2vxlan.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s]",
                        cmd.getCidr(), l2vxlan.getUuid(), l2vxlan.getName(), hostUuid);
                logger.debug(info);

                CreateVtepMsg cmsg = new CreateVtepMsg();
                cmsg.setPoolUuid(l2vxlan.getPoolUuid());
                cmsg.setClusterUuid(clusterUuid);
                cmsg.setHostUuid(hostUuid);
                cmsg.setPort(VXLAN_PORT);
                cmsg.setVtepIp(rsp.getVtepIp());
                cmsg.setType(KVM_VXLAN_TYPE);

                bus.makeTargetServiceIdByResourceUuid(cmsg, L2NetworkConstant.SERVICE_ID, l2vxlan.getPoolUuid());
                bus.send(cmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(reply.getError().toString());
                        }
                        completion.success();
                    }
                });
            }
        });
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
}
