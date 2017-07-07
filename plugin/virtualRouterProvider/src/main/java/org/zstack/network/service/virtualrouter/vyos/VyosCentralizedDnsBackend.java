package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.network.service.virtualrouter.dns.VirtualRouterCentralizedDnsBackend;

/**
 * Created by AlanJager on 2017/7/8.
 */
public class VyosCentralizedDnsBackend extends VirtualRouterCentralizedDnsBackend {
    @Override
    public NetworkServiceProviderType getProviderType() {
        return VyosConstants.PROVIDER_TYPE;
    }
}
