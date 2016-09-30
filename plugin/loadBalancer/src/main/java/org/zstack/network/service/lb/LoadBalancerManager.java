package org.zstack.network.service.lb;

/**
 * Created by frank on 8/8/2015.
 */
public interface LoadBalancerManager {
    LoadBalancerBackend getBackend(String providerType);
}
