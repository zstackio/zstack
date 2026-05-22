package org.zstack.test.integration.appliancevm

import org.junit.Test
import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.appliancevm.ApplianceVmFacadeImpl
import org.zstack.testlib.SubCase

class ApplianceVmIpv6Case extends SubCase {
    private static final String IPV4_MN_IP = "192.168.1.10"
    private static final String IPV6_MN_IP = "2001:db8::1"
    private static final String IPV4_MN_CIDR = "192.168.1.0/24"
    private static final String IPV6_MN_CIDR = "2001:db8::/64"
    private static final String UNMATCHED_VR_CIDR = "10.0.0.0/24"

    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testVrBootstrapIpv6Cidr()
        testVrBootstrapMnIpNoBrackets()
        testVrMnIpCidrMatch()
        testVrBootstrapAddressFamilyIndependent()
        testVrBootstrapFallbackWhenNoCidrMatches()
    }

    @Test
    void testVrBootstrapIpv6Cidr() {
        Map<String, Object> params = buildDualStackBootstrapParams([IPV6_MN_CIDR])

        assert params.get(ApplianceVmConstant.BootstrapParams.managementNodeIp6Cidr.toString()) == IPV6_MN_CIDR
    }

    @Test
    void testVrBootstrapMnIpNoBrackets() {
        Map<String, Object> params = buildDualStackBootstrapParams([IPV6_MN_CIDR])

        assert params.get(ApplianceVmConstant.BootstrapParams.managementNodeIp.toString()) == IPV6_MN_IP
    }

    @Test
    void testVrMnIpCidrMatch() {
        Map<String, Object> params = buildDualStackBootstrapParams([IPV6_MN_CIDR])

        assert params.get(ApplianceVmConstant.BootstrapParams.managementNodeIp.toString()) == IPV6_MN_IP
    }

    @Test
    void testVrBootstrapAddressFamilyIndependent() {
        Map<String, Object> params = buildDualStackBootstrapParams([IPV6_MN_CIDR])

        assert params.get(ApplianceVmConstant.BootstrapParams.managementNodeIp.toString()) == IPV6_MN_IP
        assert params.get(ApplianceVmConstant.BootstrapParams.managementNodeCidr.toString()) == IPV4_MN_CIDR
        assert params.get(ApplianceVmConstant.BootstrapParams.managementNodeIp6Cidr.toString()) == IPV6_MN_CIDR
    }

    @Test
    void testVrBootstrapFallbackWhenNoCidrMatches() {
        Map<String, Object> params = buildDualStackBootstrapParams([UNMATCHED_VR_CIDR])

        assert params.get(ApplianceVmConstant.BootstrapParams.managementNodeIp.toString()) == IPV4_MN_IP
    }

    private static Map<String, Object> buildDualStackBootstrapParams(Collection<String> vrManagementCidrs) {
        Map<String, Object> params = [:]
        ApplianceVmFacadeImpl.putManagementNodeBootstrapParams(
                params,
                [IPV4_MN_IP, IPV6_MN_IP],
                vrManagementCidrs,
                IPV4_MN_IP,
                null,
                IPV4_MN_CIDR,
                IPV6_MN_CIDR)
        return params
    }
}
