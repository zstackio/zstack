package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPool;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by weiwang on 20/03/2017.
 */
public class KVMRealizeL2VxlanNetworkPoolBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint{
    private static CLogger logger = Utils.getLogger(KVMRealizeL2VxlanNetworkPoolBackend.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    @Override
    public void check(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
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
        to.setBridgeName("No use");
        to.setDeviceId(nic.getDeviceId());
        to.setNicInternalName(nic.getInternalName());
        return to;
    }
}
