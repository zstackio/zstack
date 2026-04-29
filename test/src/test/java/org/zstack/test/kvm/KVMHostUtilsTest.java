package org.zstack.test.kvm;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.kvm.KVMHostUtils;

import java.util.Collections;

public class KVMHostUtilsTest {

    @Test
    public void mgmtIpOnlyWhenNoSshOutput() {
        Assert.assertEquals("192.168.1.10",
                KVMHostUtils.buildIpList("192.168.1.10", null, null));
        Assert.assertEquals("192.168.1.10",
                KVMHostUtils.buildIpList("192.168.1.10", "", null));
    }

    @Test
    public void filtersZsSuffixIface_consistentWithHostFact() {
        String out = String.join("\n",
                "1: br_zsn0    inet 172.24.250.175/16 scope global br_zsn0\\       valid_lft forever preferred_lft forever",
                "2: vnic42_eth0zs    inet 10.99.99.99/16 scope global vnic42_eth0zs\\       valid_lft forever",
                "3: docker0    inet 172.17.0.1/16 scope global docker0\\       valid_lft forever",
                "4: br_conn_all_ns    inet 169.254.64.1/18 scope global br_conn_all_ns\\       valid_lft forever",
                "5: eth1    inet 192.168.50.10/24 scope global eth1\\       valid_lft forever");
        Assert.assertEquals("172.24.250.175,172.17.0.1,192.168.50.10",
                KVMHostUtils.buildIpList("172.24.250.175", out, null));
    }

    @Test
    public void mnVipAndLoopbackRemoved() {
        String out = String.join("\n",
                "1: lo    inet 127.0.0.1/8 scope host lo\\       valid_lft forever",
                "2: eth0    inet 192.168.1.10/24 scope global eth0\\       valid_lft forever",
                "3: eth1    inet 10.0.0.99/8 scope global eth1\\       valid_lft forever");
        Assert.assertEquals("192.168.1.10",
                KVMHostUtils.buildIpList("192.168.1.10", out, "10.0.0.99"));
    }

    @Test
    public void mgmtIpAlwaysFirst() {
        String out = String.join("\n",
                "2: eth0    inet 10.0.0.5/24 scope global eth0\\       valid_lft forever",
                "3: eth1    inet 192.168.1.10/24 scope global eth1\\       valid_lft forever");
        String r = KVMHostUtils.buildIpList("192.168.1.10", out, null);
        Assert.assertTrue(r.startsWith("192.168.1.10"));
    }

    @Test
    public void duplicatedIpsDeduped() {
        String out = String.join("\n",
                "2: eth0    inet 192.168.1.10/24 scope global eth0\\       valid_lft forever",
                "3: eth1    inet 192.168.1.10/24 scope global eth1\\       valid_lft forever",
                "4: eth2    inet 10.0.0.5/24 scope global eth2\\       valid_lft forever");
        Assert.assertEquals("192.168.1.10,10.0.0.5",
                KVMHostUtils.buildIpList("192.168.1.10", out, null));
    }

    @Test
    public void handlesMalformedLines() {
        String out = String.join("\n",
                "garbage_only_one_field",
                "2: eth0    inet 10.0.0.5/24 scope global eth0\\       valid_lft forever",
                "  ",
                "3: eth1 inet6 fe80::1/64 scope link eth1\\       valid_lft forever");
        Assert.assertEquals("192.168.1.10,10.0.0.5",
                KVMHostUtils.buildIpList("192.168.1.10", out, null));
    }

    @Test
    public void zstac84446_checkAndFirstDeployUseSameSource() {
        String out = String.join("\n",
                "1: br_zsn0    inet 172.24.250.175/16 scope global br_zsn0\\       valid_lft forever",
                "2: docker0    inet 172.17.0.1/16 scope global docker0\\       valid_lft forever",
                "3: br_conn_all_ns    inet 169.254.64.1/18 scope global br_conn_all_ns\\       valid_lft forever");
        String firstDeploy = KVMHostUtils.buildIpList("172.24.250.175", out, null);
        String onCheck     = KVMHostUtils.buildIpList("172.24.250.175", out, null);
        Assert.assertEquals(firstDeploy, onCheck);
        Assert.assertTrue(firstDeploy.contains("172.17.0.1"));
        Assert.assertFalse(firstDeploy.contains("169.254.64.1"));
    }

    @Test
    public void parsesRealIpAddrOutput() {
        String out = String.join("\n",
                "1: lo    inet 127.0.0.1/8 scope host lo\\       valid_lft forever preferred_lft forever",
                "5: zsn0.2000    inet 12.1.251.206/16 brd 12.1.255.255 scope global zsn0.2000\\       valid_lft forever preferred_lft forever",
                "6: br_zsn0    inet 172.24.251.206/16 scope global dynamic br_zsn0\\       valid_lft 404688sec preferred_lft 404688sec",
                "53: br_conn_all_ns    inet 169.254.64.1/18 scope global br_conn_all_ns\\       valid_lft forever preferred_lft forever");
        Assert.assertEquals("172.24.251.206,12.1.251.206",
                KVMHostUtils.buildIpList("172.24.251.206", out, null));
    }

    @Test
    public void unionIps_detectedIsBaseAndExtraAppended() {
        Assert.assertEquals("172.24.250.175,172.17.0.1,10.0.0.7,10.0.0.8",
                KVMHostUtils.unionIps("172.24.250.175,172.17.0.1", "172.24.250.175",
                        "10.0.0.7,10.0.0.8", null, Collections.emptySet()));
    }

    @Test
    public void unionIps_fallbackToMgmtWhenDetectedEmpty() {
        Assert.assertEquals("192.168.1.10,10.0.0.7",
                KVMHostUtils.unionIps(null, "192.168.1.10", "10.0.0.7", null, Collections.emptySet()));
        Assert.assertEquals("192.168.1.10,10.0.0.7",
                KVMHostUtils.unionIps("", "192.168.1.10", "10.0.0.7", null, Collections.emptySet()));
        Assert.assertEquals("192.168.1.10,10.0.0.7",
                KVMHostUtils.unionIps("   ", "192.168.1.10", "10.0.0.7", null, Collections.emptySet()));
    }

    @Test
    public void unionIps_dropsLoopbackAndMnVip() {
        Assert.assertEquals("192.168.1.10,10.0.0.7",
                KVMHostUtils.unionIps("192.168.1.10,127.0.0.1,10.0.0.99", "192.168.1.10",
                        "127.0.0.1,10.0.0.99,10.0.0.7", "10.0.0.99", Collections.emptySet()));
    }

    @Test
    public void unionIps_detectedDeduplicatesExtra() {
        Assert.assertEquals("192.168.1.10,10.0.0.7,10.0.0.8",
                KVMHostUtils.unionIps("192.168.1.10,10.0.0.7", "192.168.1.10",
                        "10.0.0.7,10.0.0.8", null, Collections.emptySet()));
    }

    @Test
    public void unionIps_noExtraTagYieldsDetectedOnly() {
        Assert.assertEquals("192.168.1.10,172.17.0.1",
                KVMHostUtils.unionIps("192.168.1.10,172.17.0.1", "192.168.1.10",
                        null, null, Collections.emptySet()));
    }

    @Test
    public void zstac84446_deployUnionContainsAllDetected() {
        String detected = "172.24.250.175,172.17.0.1";
        String deployIps = KVMHostUtils.unionIps(detected, "172.24.250.175",
                "10.0.0.7", null, Collections.emptySet());
        for (String ip : detected.split(",")) {
            Assert.assertTrue(deployIps.contains(ip));
        }
        Assert.assertTrue(deployIps.contains("10.0.0.7"));
    }

    @Test
    public void unionIps_excludesInternalIps_whetherDetectedOrNot() {
        Assert.assertEquals("172.24.250.175",
                KVMHostUtils.unionIps("172.24.250.175", "172.24.250.175",
                        null, null, Collections.singleton("169.254.64.1")));

        Assert.assertEquals("172.24.250.175,172.17.0.1",
                KVMHostUtils.unionIps("172.24.250.175,172.17.0.1,169.254.64.1", "172.24.250.175",
                        null, null, Collections.singleton("169.254.64.1")));
    }

    @Test
    public void unionIps_excludedBeatsExtraIps() {
        Assert.assertEquals("192.168.1.10,10.0.0.7",
                KVMHostUtils.unionIps("192.168.1.10", "192.168.1.10",
                        "169.254.64.1,10.0.0.7", null,
                        Collections.singleton("169.254.64.1")));
    }

    @Test
    public void excludedInternalIps_containsBrConnAllNsOuterIp() {
        Assert.assertTrue(KVMHostUtils.EXCLUDED_INTERNAL_IPS.contains("169.254.64.1"));
    }

    @Test
    public void shouldForceTlsRedeploy_noNeedNeverForces() {
        Assert.assertFalse(KVMHostUtils.shouldForceTlsRedeploy(false, true, true));
        Assert.assertFalse(KVMHostUtils.shouldForceTlsRedeploy(false, true, false));
        Assert.assertFalse(KVMHostUtils.shouldForceTlsRedeploy(false, false, true));
        Assert.assertFalse(KVMHostUtils.shouldForceTlsRedeploy(false, false, false));
    }

    @Test
    public void shouldForceTlsRedeploy_allowRestartForces() {
        Assert.assertTrue(KVMHostUtils.shouldForceTlsRedeploy(true, true, false));
    }

    @Test
    public void shouldForceTlsRedeploy_newAddedForces() {
        Assert.assertTrue(KVMHostUtils.shouldForceTlsRedeploy(true, false, true));
    }

    @Test
    public void shouldForceTlsRedeploy_reconnectWithoutRestartSkips() {
        Assert.assertFalse(KVMHostUtils.shouldForceTlsRedeploy(true, false, false));
    }
}
