package org.zstack.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.network.service.NetworkServiceExtensionPoint;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceSpec;

public interface NetworkServiceManager {
    NetworkServiceProviderType getTypeOfNetworkServiceProviderForService(String l3NetworkUuid, NetworkServiceType serviceType);
    boolean isVmNeedNetworkService(String vmType, NetworkServiceType serviceType);

    void releaseNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPoint.NetworkServiceExtensionPosition position, Completion completion);
    void applyNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPoint.NetworkServiceExtensionPosition position, Completion completion);
}
