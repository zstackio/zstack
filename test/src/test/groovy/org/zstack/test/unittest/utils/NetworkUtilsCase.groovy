package org.zstack.test.unittest.utils

import org.junit.Test
import org.zstack.storage.ceph.MonUri
import org.zstack.utils.network.ManagementNetworkIpVersionUtils
import org.zstack.utils.network.NetworkUtils
/**
 * Created by mingjian.deng on 2017/7/20.
 */
class NetworkUtilsCase {
    static List<String> dstCidr = Arrays.asList("10.75.0.1/32", "10.75.0.2/31",
            "10.75.0.4/30", "10.75.0.8/29", "10.75.0.16/28", "10.75.0.32/27", "10.75.0.64/26", "10.75.0.128/25").sort()

    @Test
    void testIpRangeToCidr() {
        List<String> cidrs = NetworkUtils.getCidrsFromIpRange("10.75.0.1", "10.75.0.255", false)
        assert cidrs.size() == 1
        assert cidrs.get(0) == "10.75.0.0/24"

        cidrs = NetworkUtils.getCidrsFromIpRange("10.75.0.1", "10.75.0.255")
        assert cidrs.size() == dstCidr.size()
        assert cidrs.sort().toString() == dstCidr.toString()
    }

    @Test
    void testFmtCidr() {
        assert "192.168.0.0/24" == NetworkUtils.fmtCidr("192.168.0.1/24")
        assert "192.168.0.0/24" == NetworkUtils.fmtCidr("192.168.0.11/24")
        assert "192.168.0.0/24" == NetworkUtils.fmtCidr("192.168.0.123/24")
        assert "192.168.0.0/16" == NetworkUtils.fmtCidr("192.168.0.1/16")
        assert "10.0.0.0/8" == NetworkUtils.fmtCidr("10.0.0.1/8")
        assert "192.168.0.0/16" == NetworkUtils.fmtCidr("192.168.10.1/16")
        assert "128.0.0.0/1" == NetworkUtils.fmtCidr("192.168.10.1/1")
    }

    @Test
    void testGetCidrFromIpMask() {
        assert "192.168.0.0/24" == NetworkUtils.getCidrFromIpMask("192.168.0.0", "255.255.255.0")
        assert "192.168.0.0/24" == NetworkUtils.getCidrFromIpMask("192.168.0.1", "255.255.255.0")
        assert "192.168.0.128/29" == NetworkUtils.getCidrFromIpMask("192.168.0.130", "255.255.255.248")
    }

    @Test
    void testManagementEndpointIpVersion() {
        assert ManagementNetworkIpVersionUtils.normalizeIpVersion("IPv4") == ManagementNetworkIpVersionUtils.IPV4
        assert ManagementNetworkIpVersionUtils.normalizeIpVersion("ipv6") == ManagementNetworkIpVersionUtils.IPV6
        assert ManagementNetworkIpVersionUtils.normalizeIpVersion("dual") == null
        assert ManagementNetworkIpVersionUtils.getEndpointIpVersion("172.24.1.10") == ManagementNetworkIpVersionUtils.IPV4
        assert ManagementNetworkIpVersionUtils.getEndpointIpVersion("172.24.1.10:/data") == ManagementNetworkIpVersionUtils.IPV4
        assert ManagementNetworkIpVersionUtils.getEndpointIpVersion("[fd00:172:24::10]:/data") == ManagementNetworkIpVersionUtils.IPV6
        assert ManagementNetworkIpVersionUtils.getEndpointIpVersion("http://[fd00:172:24::10]:8080/path") == ManagementNetworkIpVersionUtils.IPV6
        assert ManagementNetworkIpVersionUtils.getEndpointIpVersion("storage.example.com:/data") == null
        assert ManagementNetworkIpVersionUtils.isIpv6LinkLocalEndpoint("[fe80::10]:/data")
        assert ManagementNetworkIpVersionUtils.isIpv6LinkLocalEndpoint("http://[fe80::10%eth0]:8080/path")
        assert !ManagementNetworkIpVersionUtils.isIpv6LinkLocalEndpoint("[fd00:172:24::10]:/data")
        assert !ManagementNetworkIpVersionUtils.isIpv6LinkLocalEndpoint("fe80.example.com:/data")
    }

    @Test
    void testCephMonUriIpv6Host() {
        MonUri uri = new MonUri("root:password@[fd00:172:24::10]:2222/?monPort=6789")

        assert uri.hostname == "fd00:172:24::10"
        assert uri.sshPort == 2222
        assert uri.monPort == 6789

        uri = new MonUri("root:password@[fd00:172:24:0:0:0:0:10]:2222/?monPort=6789")
        assert uri.hostname == "fd00:172:24::10"
    }
}
