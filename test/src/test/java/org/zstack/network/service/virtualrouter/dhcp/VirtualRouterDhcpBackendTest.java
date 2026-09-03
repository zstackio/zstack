package org.zstack.network.service.virtualrouter.dhcp;

import org.junit.Test;
import org.zstack.header.network.service.DhcpStruct;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class VirtualRouterDhcpBackendTest {
    @Test
    public void skipsConsecutiveIpv6EntriesWithoutRecursion() {
        List<DhcpStruct> entries = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) {
            DhcpStruct ipv6 = new DhcpStruct();
            ipv6.setIpVersion(IPv6Constants.IPv6);
            entries.add(ipv6);
        }

        DhcpStruct ipv4 = new DhcpStruct();
        ipv4.setIpVersion(IPv6Constants.IPv4);
        entries.add(ipv4);

        Iterator<DhcpStruct> iterator = entries.iterator();
        assertSame(ipv4, VirtualRouterDhcpBackend.nextIpv4DhcpStruct(iterator));
        assertNull(VirtualRouterDhcpBackend.nextIpv4DhcpStruct(iterator));
    }
}
