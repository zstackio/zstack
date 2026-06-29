package org.zstack.kvm

import org.junit.Test

import java.net.InetSocketAddress

class KVMHostFactoryIpv6Case {
    @Test
    void testExtractRemoteManagementIpSupportsIpv4AndIpv6() {
        assert KVMHostFactory.extractRemoteManagementIp(new InetSocketAddress("192.168.10.10", 7123)) == "192.168.10.10"
        assert KVMHostFactory.extractRemoteManagementIp(new InetSocketAddress("2001:db8::10", 7123)) == "2001:db8::10"
    }

    @Test
    void testTcpServerBindAddressUsesWildcardAddress() {
        InetSocketAddress bindAddress = KVMHostFactory.makeTcpServerBindAddress(7123)

        assert bindAddress.port == 7123
        assert bindAddress.address.anyLocalAddress
    }
}
