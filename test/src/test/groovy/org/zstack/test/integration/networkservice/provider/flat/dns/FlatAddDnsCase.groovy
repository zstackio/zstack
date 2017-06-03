package org.zstack.test.integration.networkservice.provider.flat.dns

import org.springframework.http.HttpEntity
import org.zstack.network.service.flat.FlatDnsBackend
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by weiwang on 25/05/2017.
 */
class FlatAddDnsCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3
    String dns1 = "8.8.8.8"
    String dns2 = "8.8.4.4"
    String dns3 = "1.2.4.8"

    @Override
    void clean() {
        env.cleanSimulatorHandlers();
        env.delete()
    }


    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
    }

    void testAddDns() {
        FlatDnsBackend.SetDnsCmd cmd = null

        env.afterSimulator(FlatDnsBackend.SET_DNS_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDnsBackend.SetDnsCmd.class)
            return rsp
        }

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns1
        }

        assert cmd != null
        assert cmd.dns.contains(dns1)

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns2
        }

        assert cmd != null
        assert cmd.dns.containsAll([dns1, dns2])
    }

    void testRemoveDns() {
        FlatDnsBackend.SetDnsCmd cmd = null

        env.afterSimulator(FlatDnsBackend.SET_DNS_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDnsBackend.SetDnsCmd.class)
            return rsp
        }

        removeDnsFromL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns1
        }

        assert cmd != null
        assert cmd.dns.contains(dns2)
        assert !cmd.dns.contains(dns1)

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns3
        }

        assert cmd != null
        assert cmd.dns.containsAll([dns2, dns3])
        assert !cmd.dns.contains(dns1)
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testAddDns()
            testRemoveDns()
        }
    }
}
