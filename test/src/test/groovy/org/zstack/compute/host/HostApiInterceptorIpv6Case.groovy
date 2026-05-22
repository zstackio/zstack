package org.zstack.compute.host

import org.junit.Test
import org.zstack.kvm.APIAddKVMHostMsg

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_HOST_10128
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_HOST_10129

class HostApiInterceptorIpv6Case {
    private static final String GLOBAL_IPV6_FULL = "2001:0db8:0000:0000:0000:0000:0000:0010"
    private static final String GLOBAL_IPV6_CANONICAL = "2001:db8::10"
    private static final String LINK_LOCAL_IPV6 = "fe80::1"
    private static final String LOOPBACK_IPV6 = "::1"
    private static final String ANY_LOCAL_IPV6 = "::"
    private static final String MULTICAST_IPV6 = "ff02::1"
    private static final String INVALID_MANAGEMENT_IP = "not-an-ip!!"

    @Test
    void testAddHostManagementIpv6CanonicalizationAndInvalidEndpointDetection() {
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg()
        msg.managementIp = GLOBAL_IPV6_FULL

        HostApiInterceptor.validateManagementEndpoint(msg)

        assert msg.managementIp == GLOBAL_IPV6_CANONICAL
        assert HostApiInterceptor.getManagementEndpointValidationErrorCode(INVALID_MANAGEMENT_IP) == ORG_ZSTACK_COMPUTE_HOST_10128
        assert HostApiInterceptor.getManagementEndpointValidationErrorCode(LOOPBACK_IPV6) == ORG_ZSTACK_COMPUTE_HOST_10129
        assert HostApiInterceptor.getManagementEndpointValidationErrorCode(LINK_LOCAL_IPV6) == ORG_ZSTACK_COMPUTE_HOST_10129
        assert HostApiInterceptor.getManagementEndpointValidationErrorCode(ANY_LOCAL_IPV6) == ORG_ZSTACK_COMPUTE_HOST_10129
        assert HostApiInterceptor.getManagementEndpointValidationErrorCode(MULTICAST_IPV6) == ORG_ZSTACK_COMPUTE_HOST_10129
    }
}
