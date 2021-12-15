package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.eip.GetEipAttachableL3UuidsForVmNicExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterStruct;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosEipBackend extends VirtualRouterEipBackend implements GetEipAttachableL3UuidsForVmNicExtensionPoint {
    @Autowired
    protected NetworkServiceManager nsMgr;
    @Autowired
    VirtualRouterManager vrMgr;

    @Override
    protected void acquireVirtualRouterVm(VirtualRouterStruct struct, ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        struct.setApplianceVmType(VyosConstants.VYOS_VM_TYPE);
        struct.setProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        struct.setVirtualRouterOfferingSelector(new VyosOfferingSelector());
        struct.setApplianceVmAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);
        super.acquireVirtualRouterVm(struct, completion);
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public List<String> getEipAttachableL3UuidsForVmNic(VmNicInventory vmNicInv, L3NetworkVO l3Network) {
        if (l3Network.getCategory().toString().equals(L3NetworkCategory.Public.toString()) || l3Network.getCategory().toString().equals(L3NetworkCategory.System.toString())){
            return new ArrayList<>();
        }

        NetworkServiceProviderType providerType = nsMgr.getTypeOfNetworkServiceProviderForService(
                l3Network.getUuid(), EipConstant.EIP_TYPE);
        if (providerType != VyosConstants.PROVIDER_TYPE) {
            /* only vrouter l3 handled here */
            return new ArrayList<>();
        }

        /* get candidate l3 networks:
         * 1. not l3 attached to vm
         * 2. must attached same clusters as vm
         * 3. pubL3  */
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmNicInv.getVmInstanceUuid()).find();

        String hostUuid = vm.getHostUuid() != null ? vm.getHostUuid() : vm.getLastHostUuid();
        if (hostUuid == null) {
            return new ArrayList<>();
        }

        List<String> vmL3NetworkUuids = vm.getVmNics().stream().map(VmNicVO::getL3NetworkUuid).collect(Collectors.toList());
        String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, hostUuid).findValue();
        List<String> l2NetworkUuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid).listValues();
        List<String> l3Uuids = Q.New(L3NetworkVO.class).in(L3NetworkVO_.l2NetworkUuid, l2NetworkUuids)
                .notIn(L3NetworkVO_.uuid, vmL3NetworkUuids).select(L3NetworkVO_.uuid).listValues();

        List<String> finalL3Uuids = vrMgr.getPublicL3UuidsOfPrivateL3(l3Network).stream()
                .filter(uuid -> l3Uuids.contains(uuid)).collect(Collectors.toList());

        return finalL3Uuids;
    }
}
