package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by weiwang on 03/03/2017.
 */
@PythonClass
public class VxlanNetworkPoolConstant {
    @PythonClass
    public static final String VXLAN_NETWORK_POOL_TYPE = "VxlanNetworkPool";
    @PythonClass
    public static final String VXLAN_NETWORK_TYPE = "VxlanNetwork";
    @PythonClass
    public static final String RANDOM_VNI_ALLOCATOR_STRATEGY = "RandomVniAllocatorStrategy";

    public static final String ACTION_CATEGORY = "vxlan";

    public static final String L2_VXLAN_NETWORK_POOL_FACTORY_SERVICE_ID = "network.l2.vxlan";
}
