package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.CheckVlanBridgeResponse;
import org.zstack.kvm.KVMAgentCommands.CreateVlanBridgeCmd;
import org.zstack.kvm.KVMAgentCommands.CreateVlanBridgeResponse;
import org.zstack.kvm.KVMAgentCommands.NicTO;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.service.MtuGetter;
import org.zstack.sdnController.header.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class KVMRealizeHardwareVxlanNetworkBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMRealizeHardwareVxlanNetworkBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private L2NetworkManager l2Mgr;

    private String makeBridgeName(String l2Uuid, int vlan) {
        return KVMHostUtils.getNormalizedBridgeName(l2Uuid, "br_%s_" + vlan);
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final L2VxlanNetworkInventory vxlan = (L2VxlanNetworkInventory) l2Network;
        HostInventory host = HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class));

        HardwareVxlanHelper.VxlanHostMappingStruct struct = HardwareVxlanHelper.getHardwareVxlanMappingVxlanId(vxlan, host);
        Integer vlanId = struct.getVlanId();
        String physicalInterface = struct.getPhysicalInterface();

        final CreateVlanBridgeCmd cmd = new CreateVlanBridgeCmd();
        cmd.setPhysicalInterfaceName(physicalInterface);
        cmd.setBridgeName(makeBridgeName(vxlan.getUuid(), vlanId));
        cmd.setVlan(vlanId);
        cmd.setMtu(new MtuGetter().getL2Mtu(vxlan));
        cmd.setL2NetworkUuid(l2Network.getUuid());

        int finalVlanId = vlanId;
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setNoStatusCheck(noStatusCheck);
        msg.setPath(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                CreateVlanBridgeResponse rsp = hreply.toResponse(CreateVlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to create bridge[%s] for hardwareVxlan[uuid:%s, type:%s, vlan:%s] on kvm host[uuid:%s], because %s",
                            cmd.getBridgeName(), l2Network.getUuid(), l2Network.getType(), finalVlanId, hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format(
                        "successfully realize bridge[%s] for hardwareVxlan[uuid:%s, type:%s, vlan:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), finalVlanId, hostUuid);
                logger.debug(info);

                SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(l2Network.getUuid());
                creator.inherent = true;
                creator.ignoreIfExisting = true;
                creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, cmd.getBridgeName())));
                creator.create();

                //

                completion.success();
            }
        });
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    public void check(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final L2VxlanNetworkInventory vxlan = (L2VxlanNetworkInventory) l2Network;

        HostInventory host = HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class));
        HardwareVxlanHelper.VxlanHostMappingStruct struct = HardwareVxlanHelper.getHardwareVxlanMappingVxlanId(vxlan, host);
        Integer vlanId = struct.getVlanId();
        String physicalInterface = struct.getPhysicalInterface();

        final KVMAgentCommands.CheckVlanBridgeCmd cmd = new KVMAgentCommands.CheckVlanBridgeCmd();
        cmd.setVlan(vlanId);
        cmd.setPhysicalInterfaceName(physicalInterface);
        cmd.setBridgeName(makeBridgeName(l2Network.getUuid(), vlanId));
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_CHECK_L2VLAN_NETWORK_PATH);
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
                CheckVlanBridgeResponse rsp = hreply.toResponse(CheckVlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to check bridge[%s] for hardwareVxlan[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                                    cmd.getBridgeName(), vxlan.getUuid(), vxlan.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked bridge[%s] for hardwareVxlan[uuid:%s, name:%s] on kvm host[uuid:%s]",
                        cmd.getBridgeName(), vxlan.getUuid(), vxlan.getName(), hostUuid);
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
        return L2NetworkType.valueOf(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE);
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
    public VSwitchType getSupportedVSwitchType() {
        return VSwitchType.valueOf(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE);
    }


    @Override
	public NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic) {
        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(dbf.findByUuid(l2Network.getUuid(), VxlanNetworkVO.class));
        VmInstanceVO vm = dbf.findByUuid(nic.getVmInstanceUuid(), VmInstanceVO.class);

        /* TODO vm must have hostUuid */
        HostInventory host = HostInventory.valueOf(dbf.findByUuid(vm.getHostUuid(), HostVO.class));
        HardwareVxlanHelper.VxlanHostMappingStruct struct = HardwareVxlanHelper.getHardwareVxlanMappingVxlanId(vxlan, host);
        Integer vlanId = struct.getVlanId();

        NicTO to = new NicTO();
		to.setMac(nic.getMac());
        to.setUuid(nic.getUuid());
		to.setBridgeName(makeBridgeName(l2Network.getUuid(), vlanId));
		to.setDeviceId(nic.getDeviceId());
		to.setNicInternalName(nic.getInternalName());
		to.setMetaData(String.valueOf(vlanId));
        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));
		return to;
	}

    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), VxlanNetworkVO.class);
       /*
       * to be done */

        return makeBridgeName(l2Network.getUuid(), vo.getVni());
    }

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }
}
