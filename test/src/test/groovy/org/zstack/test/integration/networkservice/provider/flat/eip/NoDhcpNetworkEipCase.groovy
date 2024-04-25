package org.zstack.test.integration.networkservice.provider.flat.eip

import org.zstack.sdk.EipInventory
import org.zstack.sdk.FreeIpInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.*
import org.zstack.utils.network.IPv6Constants

/**
 * Created by shixin.ruan on 2024/09/02.
 */
class NoDhcpNetworkEipCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
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


    @Override
    void test() {
        env.create {
            testAttachEipToNicWithoutGateway()
        }
    }

    void testAttachEipToNicWithoutGateway() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmNicInventory nic1 = vm.vmNics.get(0)
        detachL3NetworkFromVm {
            vmNicUuid = nic1.uuid
        }
        IpRangeInventory ipr = l3.ipRanges.get(0)
        deleteIpRange {
            uuid = ipr.uuid
        }

        /* disable dhcp will not allocate ip address to vmnic */
        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = pubL3.getUuid()
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>
        String ip = freeIp4s.get(0).getIp()
        String netmask = freeIp4s.get(0).getNetmask()
        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            service = 'DHCP'
        }
        attachL3NetworkToVm {
            l3NetworkUuid = l3.uuid
            vmInstanceUuid = vm.uuid
            systemTags = [String.format("staticIp::%s::%s", l3.uuid, ip),
                          String.format("ipv4Netmask::%s::%s", l3.uuid, netmask)]
        }

        vm = queryVmInstance { conditions = ["uuid=${vm.uuid}"] }[0]
        nic1 = vm.vmNics.get(0)
        VipInventory vip = createVip {
            name = "vip1"
            l3NetworkUuid = pubL3.uuid
        }

        expect(AssertionError.class) {
            createEip {
                name = "eip4"
                vipUuid = vip.uuid
                vmNicUuid = nic1.uuid
            }
        }

        EipInventory eip = createEip {
            name = "eip4"
            vipUuid = vip.uuid
        }

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip.uuid
                vmNicUuid = nic1.uuid
            }
        }
    }
}
