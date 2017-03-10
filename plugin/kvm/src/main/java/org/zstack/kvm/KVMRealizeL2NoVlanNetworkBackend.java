package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.errorcode.schema.Error;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands.CheckBridgeResponse;
import org.zstack.kvm.KVMAgentCommands.CreateBridgeCmd;
import org.zstack.kvm.KVMAgentCommands.CreateBridgeResponse;
import org.zstack.kvm.KVMAgentCommands.NicTO;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class KVMRealizeL2NoVlanNetworkBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMRealizeL2NoVlanNetworkBackend.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private String makeBridgeName(String physicalInterfaceName) {
        return "br_" + physicalInterfaceName;
    }

    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final CreateBridgeCmd cmd = new CreateBridgeCmd();
        cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        cmd.setBridgeName(makeBridgeName(l2Network.getPhysicalInterface()));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH);
        msg.setNoStatusCheck(noStatusCheck);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                CreateBridgeResponse rsp = hreply.toResponse(CreateBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr(
                            "failed to create bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s], because %s", cmd
                                    .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format(
                        "successfully realize bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid);
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
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    public void check(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final KVMAgentCommands.CheckBridgeCmd cmd = new KVMAgentCommands.CheckBridgeCmd();
        cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        cmd.setBridgeName(makeBridgeName(l2Network.getPhysicalInterface()));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setNoStatusCheck(noStatusCheck);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setHostUuid(hostUuid);
        msg.setPath(KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                CheckBridgeResponse rsp = hreply.toResponse(CheckBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to check bridge[%s] for l2NoVlanNetwork[uuid:%s, name:%s] on kvm host[uuid: %s], %s",
                            cmd.getBridgeName(), l2Network.getUuid(), l2Network.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked bridge[%s] for l2NoVlanNetwork[uuid:%s, name:%s] on kvm host[uuid: %s]",
                        cmd.getBridgeName(), l2Network.getUuid(), l2Network.getName(), hostUuid);
                logger.debug(info);
                completion.success();
            }
        });
    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        check(l2Network, hostUuid, false, completion);
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    }

    @Override
    public NicTO completeNicInformation(L2NetworkInventory l2Network, VmNicInventory nic) {
        NicTO to = new NicTO();
        to.setMac(nic.getMac());
        to.setUuid(nic.getUuid());
        to.setBridgeName(makeBridgeName(l2Network.getPhysicalInterface()));
        to.setDeviceId(nic.getDeviceId());
        to.setNicInternalName(nic.getInternalName());
        return to;
    }
}
