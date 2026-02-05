package org.zstack.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceExtensionPoint;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;
import java.util.Set;

public interface NetworkServiceManager {
    NetworkServiceProviderType getTypeOfNetworkServiceProviderForService(String l3NetworkUuid, NetworkServiceType serviceType);
    boolean isVmNeedNetworkService(String vmType, NetworkServiceType serviceType);

    void releaseNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPoint.NetworkServiceExtensionPosition position, Completion completion);
    void applyNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPoint.NetworkServiceExtensionPosition position, Completion completion);
    List<String> getL3NetworkDns(String l3NetworkUuid);

    /**
     * Get DNS servers for a VM NIC.
     * Priority: VM NIC system tag > L3 Network DNS
     *
     * @param vmUuid VM instance UUID
     * @param l3NetworkUuid L3 network UUID
     * @return List of DNS server addresses
     */
    List<String> getVmNicDns(String vmUuid, String l3NetworkUuid);

    void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType,
                              NetworkServiceType nsType, List<String> systemTags, Completion completion);

    void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, NetworkServiceType nsType, Completion completion);

    Set<String> getSupportedVmTypes();
}
