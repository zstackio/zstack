package org.zstack.network.service.lb;

public interface LoadBalancerFactory {
    String getType();

    LoadBalancerVO persistLoadBalancer(APICreateLoadBalancerMsg msg);
    void deleteLoadBalancer(LoadBalancerVO vo);

    String getNetworkServiceType();

    LoadBalancerBackend getLoadBalancerBackend(LoadBalancerVO vo);

    String getProviderTypeByVmNicUuid(String nicUuid);
}
