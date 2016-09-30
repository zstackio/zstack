package org.zstack.header.network.l3;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface L3NetworkConstant {
    public static final String SERVICE_ID = "network.l3";

    public static final String ACTION_CATEGORY = "l3Network";
    @PythonClass
    public static final String L3_BASIC_NETWORK_TYPE = "L3BasicNetwork";
    @PythonClass
    public static final String FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY = "FirstAvailableIpAllocatorStrategy";
    @PythonClass
    public static final String RANDOM_IP_ALLOCATOR_STRATEGY = "RandomIpAllocatorStrategy";

    public static final String QUOTA_L3_NUM = "l3.num";
}
