package org.zstack.test.integration.networkservice.provider.virtualrouter.dns

import org.springframework.http.HttpEntity
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by weiwang on 25/05/2017.
 */
class VirtualRouterAddDnsCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3
    String dns1 = "8.8.8.8"
    String dns2 = "8.8.4.4"

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
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    void testAddDns() {
        VirtualRouterCommands.SetDnsCmd cmd = null

        env.afterSimulator(VirtualRouterConstant.VR_SET_DNS_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.SetDnsCmd.class)
            return rsp
        }

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns1
        }

        assert cmd != null
        assert cmd.dns.stream()
                      .map { dnsinfo -> dnsinfo.dnsAddress }
                      .collect()
                      .contains(dns1)

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns2
        }

        assert cmd != null
        assert cmd.dns.stream()
                      .map { dnsinfo -> dnsinfo.dnsAddress }
                      .collect()
                      .containsAll([dns1, dns2])
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testAddDns()
        }
    }
}
