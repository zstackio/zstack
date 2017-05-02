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
    public static final String RANDOM_VNI_ALLOCATOR_STRATEGY = "RandomVniAllocatorStrategy";
    @PythonClass
    public static final Integer VXLAN_PORT = 4789;

    public static final String ACTION_CATEGORY = "vxlan";
}
