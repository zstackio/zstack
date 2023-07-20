package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands.NicTO;
import org.zstack.kvm.KVMCompleteNicInformationExtensionPoint;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostUtils;
import org.zstack.network.service.MtuGetter;
import org.zstack.sdnController.header.SdnControllerConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

public class KVMRealizeHardwareVxlanPoolNetworkBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMRealizeHardwareVxlanPoolNetworkBackend.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private String makeBridgeName(String l2Uuid) {
        return KVMHostUtils.getNormalizedBridgeName(l2Uuid, "br_%s");
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        completion.success();
    }

    public void check(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        CheckNetworkPhysicalInterfaceMsg msg = new CheckNetworkPhysicalInterfaceMsg();
        msg.setHostUuid(hostUuid);
        msg.setPhysicalInterface(l2Network.getPhysicalInterface());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CheckNetworkPhysicalInterfaceReply rsp = reply.castReply();
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to check physical interface for HardwareVxlanPool[uuid:%s, name:%s] on kvm host[uuid: %s], %s",
                            l2Network.getUuid(), l2Network.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked physical interface for HardwareVxlanPool[uuid:%s, name:%s] on kvm host[uuid: %s]",
                        l2Network.getUuid(), l2Network.getName(), hostUuid);
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
        return L2NetworkType.valueOf(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public VSwitchType getSupportedVSwitchType() {
        return VSwitchType.valueOf(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE);
    }

    @Override
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return L2NetworkType.valueOf(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE);
    }
    @Override
    public NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic) {
        NicTO to = new NicTO();
        to.setMac(nic.getMac());
        to.setUuid(nic.getUuid());
        to.setBridgeName(makeBridgeName(l2Network.getUuid()));
        to.setDeviceId(nic.getDeviceId());
        to.setNicInternalName(nic.getInternalName());
        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));
        return to;
    }

    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        return null;
    }

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }
}
