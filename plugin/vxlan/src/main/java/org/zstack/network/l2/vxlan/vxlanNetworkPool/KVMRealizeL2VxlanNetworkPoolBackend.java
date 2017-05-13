package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
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
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.network.l2.vxlan.vtep.CreateVtepMsg;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.KVM_VXLAN_TYPE;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH;
import static org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant.VXLAN_PORT;

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

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        check(l2Network, hostUuid, true, completion);
    }

    public void check(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        final L2VxlanNetworkPoolInventory vxlanPool = (L2VxlanNetworkPoolInventory) l2Network;
        final String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();

        VxlanKvmAgentCommands.CheckVxlanCidrCmd cmd = new VxlanKvmAgentCommands.CheckVxlanCidrCmd();
        cmd.setCidr(getAttachedCidrs(vxlanPool.getUuid()).get(clusterUuid));
        if (!l2Network.getPhysicalInterface().isEmpty()) {
            cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        }

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    ErrorCode err = operr("failed to check cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                            cmd.getCidr(), vxlanPool.getUuid(), vxlanPool.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked cidr[%s] for l2VxlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s]",
                        cmd.getCidr(), vxlanPool.getUuid(), vxlanPool.getName(), hostUuid);
                logger.debug(info);

                completion.success();
            }
        });

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
