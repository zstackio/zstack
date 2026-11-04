package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.CheckVlanBridgeResponse;
import org.zstack.kvm.KVMAgentCommands.CreateVlanBridgeCmd;
import org.zstack.kvm.KVMAgentCommands.CreateVlanBridgeResponse;
import org.zstack.kvm.KVMAgentCommands.CreateVlanBridgesCmd;
import org.zstack.kvm.KVMAgentCommands.CreateVlanBridgesResponse;
import org.zstack.kvm.KVMAgentCommands.NicTO;
import org.zstack.network.l2.L2NetworkGlobalConfig;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.service.MtuGetter;
import org.zstack.sdnController.header.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        HardwareL2VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), HardwareL2VxlanNetworkVO.class);
        final HardwareL2VxlanNetworkInventory vxlan = HardwareL2VxlanNetworkInventory.valueOf(vo);
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
        cmd.setIsolated(vxlan.getIsolated());

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

    @Override
    public void realize(final List<L2NetworkInventory> l2Networks, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        if (l2Networks == null || l2Networks.isEmpty()) {
            completion.success();
            return;
        }

        List<String> missingVoUuids = l2Networks.stream()
                .filter(l2 -> !(l2 instanceof HardwareL2VxlanNetworkInventory))
                .map(L2NetworkInventory::getUuid)
                .collect(Collectors.toList());
        List<HardwareL2VxlanNetworkVO> missingVos = missingVoUuids.isEmpty() ? Collections.emptyList() :
                Q.New(HardwareL2VxlanNetworkVO.class)
                        .in(HardwareL2VxlanNetworkVO_.uuid, missingVoUuids)
                        .list();
        Map<String, HardwareL2VxlanNetworkVO> voMap = missingVos.stream()
                .collect(Collectors.toMap(HardwareL2VxlanNetworkVO::getUuid, Function.identity()));

        List<HardwareL2VxlanNetworkInventory> vxlans = new ArrayList<>(l2Networks.size());
        for (L2NetworkInventory l2 : l2Networks) {
            if (l2 instanceof HardwareL2VxlanNetworkInventory) {
                vxlans.add((HardwareL2VxlanNetworkInventory) l2);
            } else {
                HardwareL2VxlanNetworkVO vo = voMap.get(l2.getUuid());
                if (vo != null) {
                    vxlans.add(HardwareL2VxlanNetworkInventory.valueOf(vo));
                }
            }
        }

        HostInventory host = HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class));
        Map<String, HardwareVxlanHelper.VxlanHostMappingStruct> mappingMap =
                HardwareVxlanHelper.getHardwareVxlanMappingVxlanIds(vxlans, host);
        List<String> l2Uuids = vxlans.stream().map(HardwareL2VxlanNetworkInventory::getUuid).collect(Collectors.toList());
        Map<String, String> existingBridgeNames = KVMHostUtils.getExistingBridgeNames(l2Uuids);
        Map<String, Integer> mtuMap = new MtuGetter().getL2Mtus(new ArrayList<>(vxlans));

        final List<CreateVlanBridgeCmd> allCmds = new ArrayList<>(vxlans.size());
        for (HardwareL2VxlanNetworkInventory vxlan : vxlans) {
            HardwareVxlanHelper.VxlanHostMappingStruct struct = mappingMap.get(vxlan.getUuid());
            Integer vlanId = struct.getVlanId();
            String physicalInterface = struct.getPhysicalInterface();

            CreateVlanBridgeCmd cmd = new CreateVlanBridgeCmd();
            cmd.setPhysicalInterfaceName(physicalInterface);
            String bridgeName = existingBridgeNames.get(vxlan.getUuid());
            if (bridgeName == null) {
                bridgeName = makeBridgeName(vxlan.getUuid(), vlanId);
            }
            cmd.setBridgeName(bridgeName);
            cmd.setVlan(vlanId);
            cmd.setMtu(mtuMap.get(vxlan.getUuid()));
            cmd.setL2NetworkUuid(vxlan.getUuid());
            cmd.setIsolated(vxlan.getIsolated());
            allCmds.add(cmd);
        }

        int step = L2NetworkGlobalConfig.RealizeL2NetworkBatchCount.value(Integer.class);
        List<CreateVlanBridgesCmd> batches = new ArrayList<>();
        for (int i = 0; i < allCmds.size(); i += step) {
            CreateVlanBridgesCmd batchCmd = new CreateVlanBridgesCmd();
            batchCmd.setBridgeCmds(new ArrayList<>(allCmds.subList(i, Math.min(i + step, allCmds.size()))));
            batches.add(batchCmd);
        }

        new While<>(batches).each((batchCmd, c) -> {
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(hostUuid);
            msg.setCommand(batchCmd);
            msg.setNoStatusCheck(noStatusCheck);
            msg.setPath(KVMConstant.KVM_REALIZE_L2VLAN_NETWORKS_PATH);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(msg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        c.addError(reply.getError());
                        c.allDone();
                        return;
                    }

                    KVMHostAsyncHttpCallReply hreply = reply.castReply();
                    CreateVlanBridgesResponse rsp = hreply.toResponse(CreateVlanBridgesResponse.class);
                    if (!rsp.isSuccess()) {
                        ErrorCode err = operr(
                                "failed to create bridges for hardwareVxlan on kvm host[uuid:%s], because %s",
                                hostUuid, rsp.getError());
                        c.addError(err);
                        c.allDone();
                        return;
                    }
                    c.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                for (CreateVlanBridgeCmd cmd : allCmds) {
                    if (!existingBridgeNames.containsKey(cmd.getL2NetworkUuid())) {
                        SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(cmd.getL2NetworkUuid());
                        creator.inherent = true;
                        creator.ignoreIfExisting = true;
                        creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, cmd.getBridgeName())));
                        creator.create();
                    }
                }
                completion.success();
            }
        });
    }

    public void check(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        HardwareL2VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), HardwareL2VxlanNetworkVO.class);
        final HardwareL2VxlanNetworkInventory vxlan = HardwareL2VxlanNetworkInventory.valueOf(vo);

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
        HardwareL2VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), HardwareL2VxlanNetworkVO.class);
        final HardwareL2VxlanNetworkInventory vxlan = HardwareL2VxlanNetworkInventory.valueOf(vo);
        VmInstanceVO vm = dbf.findByUuid(nic.getVmInstanceUuid(), VmInstanceVO.class);

        /* TODO vm must have hostUuid */
        HostInventory host = HostInventory.valueOf(dbf.findByUuid(vm.getHostUuid(), HostVO.class));
        HardwareVxlanHelper.VxlanHostMappingStruct struct = HardwareVxlanHelper.getHardwareVxlanMappingVxlanId(vxlan, host);
        Integer vlanId = struct.getVlanId();

        NicTO to = KVMAgentCommands.NicTO.fromVmNicInventory(nic);
		to.setBridgeName(makeBridgeName(l2Network.getUuid(), vlanId));
		to.setMetaData(String.valueOf(vlanId));
        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));
		return to;
	}

    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        HardwareL2VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), HardwareL2VxlanNetworkVO.class);
       /*
       * to be done */

        return makeBridgeName(l2Network.getUuid(), vo.getVni());
    }

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        HardwareL2VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), HardwareL2VxlanNetworkVO.class);
        HardwareL2VxlanNetworkInventory l2Vxlan = HardwareL2VxlanNetworkInventory.valueOf(vo);
        KVMAgentCommands.DeleteVlanBridgeCmd cmd = new KVMAgentCommands.DeleteVlanBridgeCmd();
        cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        cmd.setBridgeName(makeBridgeName(l2Vxlan.getUuid(), l2Vxlan.getVlan()));
        cmd.setVlan(l2Vxlan.getVlan());
        cmd.setL2NetworkUuid(l2Network.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_DELETE_L2VLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.DeleteVlanBridgeResponse rsp = hreply.toResponse(KVMAgentCommands.DeleteVlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to delete bridge[%s] for l2Network[uuid:%s, type:%s, vlan:%s] on kvm host[uuid:%s], because %s",
                            cmd.getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2Vxlan.getVlan(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String message = String.format(
                        "successfully delete bridge[%s] for l2Network[uuid:%s, type:%s, vlan:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), l2Vxlan.getVlan(), hostUuid);
                logger.debug(message);

                completion.success();
            }
        });
    }
}
