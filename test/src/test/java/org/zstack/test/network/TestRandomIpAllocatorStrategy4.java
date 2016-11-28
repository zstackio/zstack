package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class TestRandomIpAllocatorStrategy4 {
    CLogger logger = Utils.getLogger(TestRandomIpAllocatorStrategy4.class);

    @Test
    public void test() {
        String startIp = "10.10.1.20";
        String endIp = "10.10.1.100";
        List<Long> empty = new ArrayList<Long>();
        for (int i = 0; i < 10000; i++) {
            String ip = NetworkUtils.randomAllocateIpv4Address(startIp, endIp, empty);
            Assert.assertTrue(NetworkUtils.isIpv4InRange(ip, startIp, endIp));
        }

        startIp = "10.10.1.1";
        endIp = "10.10.1.255";
        List<Long> used = new ArrayList<Long>();
        long len = NetworkUtils.ipRangeLength(startIp, endIp);
        for (int i = 0; i < len; i++) {
            String ip = NetworkUtils.randomAllocateIpv4Address(startIp, endIp, used);
            Assert.assertTrue(NetworkUtils.isIpv4InRange(ip, startIp, endIp));
            used.add(NetworkUtils.ipv4StringToLong(ip));
        }

        String ip = NetworkUtils.randomAllocateIpv4Address(startIp, endIp, used);
        Assert.assertNull(ip);
    }
}
