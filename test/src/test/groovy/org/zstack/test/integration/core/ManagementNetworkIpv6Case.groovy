package org.zstack.test.integration.core

import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.core.NetworkGlobalConfig
import org.zstack.core.Platform
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils
import org.zstack.utils.network.NetworkUtils
import org.junit.Test

class ManagementNetworkIpv6Case {
    private static final String IPV4 = "192.168.1.10"
    private static final String IPV6 = "2001:db8::1"
    private static final String IPV6_FULL = "2001:0db8:0000:0000:0000:0000:0000:0001"
    private static final String LINK_LOCAL_IPV6 = "fe80::1"
    private static final String INVALID_IP = "not-an-ip!!"
    private static final int REST_PORT = 8080
    private static final int JGROUP_PORT = 7805

    @Test
    void test() {
        testPreferIpv6DefaultFalse()
        testBuildUrlIpv4()
        testBuildUrlIpv6()
        testBuildHostPortIpv6()
        testBracketIpv6Idempotent()
        testNormalizeIpv6()
        testManagementEndpointValidation()
        testJGroupsInitialHostsIpv6Format()
        testJGroupsInitialHostsIpv4Regression()
        testIpv6NetworkCidr()
        testIpInCidrDualStack()
        testManagementCidrIpVersionOverload()
        testApplianceVmBootstrapParam()
    }

    void testPreferIpv6DefaultFalse() {
        assert NetworkGlobalConfig.PREFER_IPV6.getIdentity() == "managementServer.prefer.ipv6"
    }

    void testBuildUrlIpv4() {
        assert IPv6NetworkUtils.buildHttpUrl(IPV4, REST_PORT) == "http://192.168.1.10:8080"
    }

    void testBuildUrlIpv6() {
        assert IPv6NetworkUtils.buildHttpUrl(IPV6, REST_PORT) == "http://[2001:db8::1]:8080"
    }

    void testBuildHostPortIpv6() {
        assert IPv6NetworkUtils.formatHostPort(IPV6, REST_PORT) == "[2001:db8::1]:8080"
    }

    void testBracketIpv6Idempotent() {
        assert IPv6NetworkUtils.formatHostForUrl(IPV6) == "[2001:db8::1]"
        assert IPv6NetworkUtils.formatHostForUrl("[2001:db8::1]") == "[2001:db8::1]"
    }

    void testNormalizeIpv6() {
        assert IPv6NetworkUtils.normalizeIpv6(IPV6_FULL) == IPV6
    }

    void testManagementEndpointValidation() {
        assert IPv6NetworkUtils.isValidManagementEndpoint(IPV4)
        assert IPv6NetworkUtils.isValidManagementEndpoint(IPV6)
        assert IPv6NetworkUtils.isValidManagementEndpoint("host-01.example.com")
        assert !IPv6NetworkUtils.isValidManagementEndpoint(LINK_LOCAL_IPV6)
        assert !IPv6NetworkUtils.isValidManagementEndpoint(INVALID_IP)
    }

    void testJGroupsInitialHostsIpv6Format() {
        assert Platform.formatJGroupsInitialHosts(IPV6, "2001:db8::2", JGROUP_PORT) ==
                "[2001:db8::1][7805],[2001:db8::2][7805]"
    }

    void testJGroupsInitialHostsIpv4Regression() {
        assert Platform.formatJGroupsInitialHosts(IPV4, "192.168.1.11", JGROUP_PORT) ==
                "192.168.1.10[7805],192.168.1.11[7805]"
    }

    void testIpv6NetworkCidr() {
        assert NetworkUtils.getNetworkAddressFromCidr("2001:db8::1/64") == "2001:db8::/64"
    }

    void testIpInCidrDualStack() {
        assert NetworkUtils.isIpInCidr(IPV4, "192.168.1.0/24")
        assert NetworkUtils.isIpInCidr(IPV6, "2001:db8::/64")
        assert !NetworkUtils.isIpInCidr(IPV4, "2001:db8::/64")
        assert !NetworkUtils.isIpInCidr(IPV6, "192.168.1.0/24")
    }

    void testManagementCidrIpVersionOverload() {
        assert Platform.getManagementServerCidr(IPv6Constants.IPv4) == Platform.getManagementServerCidr(Platform.getManagementServerIp())
    }

    void testApplianceVmBootstrapParam() {
        assert ApplianceVmConstant.BootstrapParams.managementNodeIp6Cidr.toString() == "managementNodeIp6Cidr"
    }
}
