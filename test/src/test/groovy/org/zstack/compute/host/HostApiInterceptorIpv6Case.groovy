package org.zstack.compute.host

import org.junit.Test
import org.zstack.kvm.APIAddKVMHostMsg
import org.zstack.utils.network.IPv6NetworkUtils

class HostApiInterceptorIpv6Case {
    private static final String GLOBAL_IPV6_FULL = "2001:0db8:0000:0000:0000:0000:0000:0010"
    private static final String GLOBAL_IPV6_CANONICAL = "2001:db8::10"
    private static final String LINK_LOCAL_IPV6 = "fe80::1"
    private static final String LOOPBACK_IPV6 = "::1"
    private static final String INVALID_MANAGEMENT_IP = "not-an-ip!!"

    @Test
    void testAddHostManagementIpv6CanonicalizationAndInvalidEndpointDetection() {
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg()
        msg.managementIp = GLOBAL_IPV6_FULL

        HostApiInterceptor.validateManagementEndpoint(msg)

        assert msg.managementIp == GLOBAL_IPV6_CANONICAL
        assert !IPv6NetworkUtils.isValidManagementEndpoint(INVALID_MANAGEMENT_IP)
        assert !IPv6NetworkUtils.isValidManagementEndpoint(LOOPBACK_IPV6)
        assert !IPv6NetworkUtils.isValidManagementEndpoint(LINK_LOCAL_IPV6)
    }
}
