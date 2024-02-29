package org.zstack.network.service.virtualrouter.vip;

import org.zstack.appliancevm.ApplianceVmType;

import java.util.List;

public interface VirtualRouterVipConfigFactory {
    ApplianceVmType getApplianceVmType();
    void attachNetworkService(String vrUuid, List<String> vipUuids);
    void detachNetworkService(String vrUuid, List<String> vipUuids);
    List<String> getVrUuidsByNetworkService(String vipUuid);
    List<String> getAllVrUuidsByNetworkService(String vipUuid);
    List<String> getVipUuidsByRouterUuid(String vrUuid);
}
