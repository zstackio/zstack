package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/26.
 */
class IPv6EipCase extends SubCase {
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
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testIPv6EipApiValidator()
            testIPv6EipApplyNetworkService()
        }
    }

    void testIPv6EipApiValidator() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_statefull_1 = env.inventoryByName("l3-Statefull-DHCP-1")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory host = env.inventoryByName("kvm-1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
            hostUuid = host.uuid
        }
        VmNicInventory nic = vm.getVmNics()[0]
        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3.uuid
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        } [0]
        nic = vm.getVmNics()[0]
        UsedIpInventory ipv4
        UsedIpInventory ipv6
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                ipv4 = ip
            } else {
                ipv6 = ip
            }
        }
        assert ipv6.netmask == IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr("2001:2003::/64")

        expect(AssertionError.class) {
            VipInventory vip6 = createVip {
                name = "vip6"
                l3NetworkUuid = l3_statefull_1.uuid
                requiredIp = "192.168.2.1"
            }
        }

        expect(AssertionError.class) {
            VipInventory vip4 = createVip {
                name = "vip4"
                l3NetworkUuid = l3_1.uuid
                requiredIp = "2001:2004::2004"
            }
        }

        VipInventory vip6 = createVip {
            name = "vip6"
            l3NetworkUuid = l3_statefull_1.uuid
            requiredIp = "2001:2004::2004"
        }
        assert vip6.netmask == IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr("2001:2004::/64")

        VipInventory vip4 = createVip {
            name = "vip4"
            l3NetworkUuid = l3_1.uuid
        }

        EipInventory eip6 = createEip {
            name = "eip6"
            vipUuid = vip6.uuid
        }

        EipInventory eip4 = createEip {
            name = "eip4"
            vipUuid = vip4.uuid
        }

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip4.uuid
                vmNicUuid = nic.uuid
                usedIpUuid = ipv6.uuid
            }
        }

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip6.uuid
                vmNicUuid = nic.uuid
                usedIpUuid = ipv4.uuid
            }
        }

        attachEip {
            eipUuid = eip4.uuid
            vmNicUuid = nic.uuid
            usedIpUuid = ipv4.uuid
        }

        attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = nic.uuid
            usedIpUuid = ipv6.uuid
        }
    }

    void testIPv6EipApplyNetworkService() {
        HostInventory host = env.inventoryByName("kvm-1")

        VmInstanceInventory vm = queryVmInstance {
            conditions=["name=vm-eip"]
        } [0]

        rebootVmInstance {
            uuid = vm.uuid
        }

        reconnectHost {
            uuid = host.uuid
        }

        EipInventory eip4 = queryEip {
            conditions=["name=eip4"]
        } [0]

        detachEip {
            uuid = eip4.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        reconnectHost {
            uuid = host.uuid
        }

        EipInventory eip6 = queryEip {
            conditions=["name=eip6"]
        } [0]

        detachEip {
            uuid = eip6.uuid
        }
    }

}

