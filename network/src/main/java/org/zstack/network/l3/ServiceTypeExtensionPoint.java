package org.zstack.network.l3;

import org.zstack.header.host.HostNetworkInterfaceServiceType;

import java.util.List;

public interface ServiceTypeExtensionPoint {
    void syncManagementServiceTypeExtensionPoint(List<String> hostUuids, String interfaceName, Integer virtualNetworkId, boolean isDelete);
    Boolean checkHostServiceTypeExtensionPoint(String hostUuid, String interfaceName, List<HostNetworkInterfaceServiceType> serviceTypeList);
}

