package org.zstack.test.unittest.utils;

import org.junit.Test;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ZSTAC87063IPv6NetworkUtilsCase {
    @Test
    public void testNormalizeHost() throws Exception {
        String urlHost = new URI("http://[fd11:0005:0005:0029:0000:0000:0066:7b32]:7761/image/upload").getHost();
        assertEquals("fd11:5:5:29::66:7b32", IPv6NetworkUtils.normalizeHost(urlHost));
        assertEquals("fd11:5:5:29::66:7b32",
                IPv6NetworkUtils.normalizeHost("fd11:0005:0005:0029:0000:0000:0066:7b32"));
        assertEquals("192.168.1.10", IPv6NetworkUtils.normalizeHost("192.168.1.10"));
        assertEquals("ceph.example.com", IPv6NetworkUtils.normalizeHost("ceph.example.com"));
        assertNull(IPv6NetworkUtils.normalizeHost(null));
    }
}
