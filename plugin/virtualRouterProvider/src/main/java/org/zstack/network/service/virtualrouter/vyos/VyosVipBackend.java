package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterStruct;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipBackend;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipConfigFactory;

import java.util.List;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosVipBackend extends VirtualRouterVipBackend implements VirtualRouterVipConfigFactory {
    @Override
    protected void acquireVirtualRouterVm(VirtualRouterStruct struct, ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        struct.setApplianceVmType(VyosConstants.VYOS_VM_TYPE);
        struct.setProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        struct.setVirtualRouterOfferingSelector(new VyosOfferingSelector());
        struct.setApplianceVmAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);
        super.acquireVirtualRouterVm(struct, completion);
    }

    @Override
    public String getServiceProviderTypeForVip() {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public ApplianceVmType getApplianceVmType() {
        return ApplianceVmType.valueOf(VyosConstants.VYOS_VM_TYPE);
    }

    @Override
    public void attachNetworkService(String vrUuid, List<String> vipUuids) {
        super.attachNetworkService(vrUuid, vipUuids);
    }

    @Override
    public void detachNetworkService(String vrUuid, List<String> vipUuids) {
        super.detachNetworkService(vrUuid, vipUuids);
    }

    @Override
    public List<String> getVrUuidsByNetworkService(String vipUuid) {
        return super.getVrUuidsByNetworkService(vipUuid);
    }

    @Override
    public List<String> getVipUuidsByRouterUuid(String vrUuid) {
        return super.getVipUuidsByRouterUuid(vrUuid);
    }
}
