package org.zstack.network.service.lb;

import org.zstack.header.network.service.NetworkServiceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerConstants {
    public static final String SERVICE_ID = "loadBalancer";

    public static final String LB_NETWORK_SERVICE_TYPE_STRING = "LoadBalancer";

    public static final NetworkServiceType LB_NETWORK_SERVICE_TYPE = new NetworkServiceType(LB_NETWORK_SERVICE_TYPE_STRING);

    public static final String BALANCE_ALGORITHM_ROUND_ROBIN = "roundrobin";
    public static final String BALANCE_ALGORITHM_LEAST_CONN = "leastconn";
    public static final String BALANCE_ALGORITHM_LEAST_SOURCE = "source";

    public static final List<String> BALANCE_ALGORITHMS = new ArrayList<String>();

    public static final String HEALTH_CHECK_TARGET_PROTOCL_TCP = "tcp";

    public static final List<String> HEALTH_CHECK_TARGET_PROTOCOLS = new ArrayList<String>();

    public static final String ACTION_CATEGORY = "loadBalancer";

    public static final String QUOTA_LOAD_BALANCER_NUM = "loadBalancer.num";

    public static final String LB_PROTOCOL_TCP = "tcp";
    public static final String LB_PROTOCOL_HTTP = "http";

    static {
        BALANCE_ALGORITHMS.add(BALANCE_ALGORITHM_ROUND_ROBIN);
        BALANCE_ALGORITHMS.add(BALANCE_ALGORITHM_LEAST_CONN);
        BALANCE_ALGORITHMS.add(BALANCE_ALGORITHM_LEAST_SOURCE);

        HEALTH_CHECK_TARGET_PROTOCOLS.add(HEALTH_CHECK_TARGET_PROTOCL_TCP);
    }

}
