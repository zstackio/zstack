package org.zstack.test.integration.networkservice.provider.flat.dns

import org.springframework.http.HttpEntity
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.vm.VmInstanceSpec
import org.zstack.network.service.NetworkServiceGlobalConfig
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.sdk.GetL3NetworkMtuResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.*
import org.zstack.utils.gson.JSONObjectUtil

class FlatDnsOrderCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testDnsOder()
        }
    }

    void testDnsOder() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.1"
        }

        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.2"
        }

        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.3"
        }

        FlatDhcpBackend.ApplyDhcpCmd cmd = null
        env.simulator(FlatDhcpBackend.APPLY_DHCP_PATH){HttpEntity<String> e,EnvSpec spec ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            def rsp = new FlatDhcpBackend.ApplyDhcpRsp()
            rsp.success = true
            return rsp
        }
        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.dhcp.size() == 1
        assert cmd.dhcp.get(0).dns.size() == 3
        assert cmd.dhcp.get(0).dns.get(0) == "1.1.1.1"
        assert cmd.dhcp.get(0).dns.get(1) == "1.1.1.2"
        assert cmd.dhcp.get(0).dns.get(2) == "1.1.1.3"

        removeDnsFromL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.2"
        }

        cmd = null
        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.dhcp.size() == 1
        assert cmd.dhcp.get(0).dns.size() == 2
        assert cmd.dhcp.get(0).dns.get(0) == "1.1.1.1"
        assert cmd.dhcp.get(0).dns.get(1) == "1.1.1.3"

        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.4"
        }
        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.2"
        }
        cmd = null
        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.dhcp.size() == 1
        assert cmd.dhcp.get(0).dns.size() == 4
        assert cmd.dhcp.get(0).dns.get(0) == "1.1.1.1"
        assert cmd.dhcp.get(0).dns.get(1) == "1.1.1.3"
        assert cmd.dhcp.get(0).dns.get(2) == "1.1.1.4"
        assert cmd.dhcp.get(0).dns.get(3) == "1.1.1.2"
    }

    @Override
    void clean() {
        env.delete()
    }
}
