package org.zstack.network.service.lb;

import org.zstack.header.network.service.NetworkServiceType;

/**
 * Created by frank on 8/8/2015.
 */
public interface LoadBalancerConstants {
    String SERVICE_ID = "loadBalancer";

    String LB_NETWORK_SERVICE_TYPE_STRING = "LoadBalancer";

    NetworkServiceType LB_NETWORK_SERVICE_TYPE = new NetworkServiceType(LB_NETWORK_SERVICE_TYPE_STRING);
}
