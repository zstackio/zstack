package org.zstack.utils.test;

import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.zstack.utils.network.NetworkUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestNetworkUtils {
    @Test
    public void test() {
        System.out.println(NetworkUtils.ipv4StringToLong("10.223.110.1"));
        System.out.println(NetworkUtils.ipv4StringToLong("10.223.110.100"));
        //System.out.println(NetworkUtils.ipv4StringToLong("10.223.110.1.1"));
        long val = NetworkUtils.ipv4StringToLong("10.223.110.1");
        System.out.println(NetworkUtils.longToIpv4String(val));
        val = NetworkUtils.ipv4StringToLong("255.255.255.255");
        System.out.println(NetworkUtils.longToIpv4String(val));
        val = NetworkUtils.ipv4StringToLong("0.0.0.0");
        System.out.println(NetworkUtils.longToIpv4String(val));
        val = NetworkUtils.ipv4StringToLong("127.0.0.1");
        System.out.println(NetworkUtils.longToIpv4String(val));
        //val = NetworkUtils.ipv4StringToLong("helwor");
        //val = NetworkUtils.ipv4StringToLong("l.he.a.d");
        System.out.println(NetworkUtils.isIpv4Address("000.1.2.3"));
        System.out.println(NetworkUtils.isIpv4RangeOverlap("10.223.110.1", "10.223.110.18", "10.223.110.30", "10.223.110.60"));
        System.out.println(NetworkUtils.isIpv4RangeOverlap("10.223.110.1", "10.223.110.18", "10.223.110.18", "10.223.110.60"));
        System.out.println(NetworkUtils.isIpv4RangeOverlap("10.223.110.1", "10.223.110.18", "10.223.110.19", "10.223.110.60"));
        System.out.println(NetworkUtils.isIpv4RangeOverlap("10.223.110.1", "10.223.110.18", "10.223.100.19", "10.223.110.60"));
        System.out.println(NetworkUtils.isIpv4RangeOverlap("10.223.110.1", "10.223.110.1", "10.223.100.19", "10.223.110.60"));
        System.out.println(NetworkUtils.isIpv4RangeOverlap("10.223.110.1", "10.223.110.1", "10.223.110.1", "10.223.110.60"));
        System.out.println(3/2);

        String hname = "http://192.168.0.199";
        System.out.println(String.format("%s %s", hname, NetworkUtils.isHostname(hname)));
        hname = "hostname";
        System.out.println(String.format("%s %s", hname, NetworkUtils.isHostname(hname)));
        hname = "hostname.zstack.org";
        System.out.println(String.format("%s %s", hname, NetworkUtils.isHostname(hname)));
    }

    @Test
    public void testDomainName() {
        assertTrue(NetworkUtils.isDomainName("db.prod.local"));
        assertTrue(NetworkUtils.isDomainName("app-1.internal"));
        assertTrue(NetworkUtils.isDomainName("localhost"));

        assertFalse(NetworkUtils.isDomainName(null));
        assertFalse(NetworkUtils.isDomainName("*.app.local"));
        assertFalse(NetworkUtils.isDomainName("db_prod.local"));
        assertFalse(NetworkUtils.isDomainName("中文.local"));
        assertFalse(NetworkUtils.isDomainName(".prod.local"));
        assertFalse(NetworkUtils.isDomainName("prod.local."));
        assertFalse(NetworkUtils.isDomainName("-db.prod.local"));
        assertFalse(NetworkUtils.isDomainName("db-.prod.local"));
        assertFalse(NetworkUtils.isDomainName(new String(new char[64]).replace('\0', 'a') + ".local"));
        assertFalse(NetworkUtils.isDomainName(new String(new char[254]).replace('\0', 'a')));
    }
}
