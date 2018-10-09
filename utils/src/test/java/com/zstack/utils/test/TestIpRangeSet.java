package com.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.IpRangeSet;

import java.util.Set;

public class TestIpRangeSet {

    @Test
    public void test() {
        Set<String> ips = IpRangeSet.listAllIps("127.0.0.1-127.0.0.4, ^127.0.0.3");
        assert ips.size() == 3;
        assert ips.contains("127.0.0.1");
        assert ips.contains("127.0.0.2");
        assert ips.contains("127.0.0.4");

    }
}
