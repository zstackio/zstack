package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.kvm.KVMConstant;
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
public class L2VxlanFakeNetworkRealizer implements L2NetworkRealizationExtensionPoint {
    private static CLogger logger = Utils.getLogger(L2VxlanFakeNetworkRealizer.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        VxlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), VxlanNetworkVO.class);
        HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class);
        logger.debug(String.format("fake successfully realized l2network[uuid:%s, name:%s] on cluster[uuid:%s] with cidr [%s]",
                l2Network.getUuid(), l2Network.getName(), hvo.getClusterUuid(), getAttachedCidrs(vo.getPoolUuid()).get(hvo.getClusterUuid())));
        completion.success();
    }

    @Override
    public void check(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(VxlanNetworkPoolConstant.VXLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
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
