package org.zstack.test.integration.network.l3network.ipv6

import com.googlecode.ipv6.IPv6Address
import org.zstack.compute.vm.StaticIpOperator
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.UsedIpInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6NetworkUtils

/**
 * Created by shixin on 2018/09/10.
 */
class SetStaticIpv6AddressCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testCreateVmOfPrivateIPv6Network()
            testCreateVmWithStaticIpv6()
            testAttachL3NetworkToVmNicWithStaticIpv6()
        }
    }

    void testCreateVmOfPrivateIPv6Network() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid]
            defaultL3NetworkUuid = l3_statefull.uuid
        }

        VmNicInventory nic = vm.getVmNics().get(0)
        UsedIpInventory ip6 = nic.getUsedIps().get(0)

        stopVmInstance {
            uuid = vm.uuid
        }

        IPv6Address address2 = IPv6Address.fromString("2001:2003::02")
        IPv6Address address3 = IPv6Address.fromString("2001:2003::03")
        String ip6Str = (ip6.ip == address2.toString() ? address3.toString() : address2.toString())
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            ip = ip6Str
        }

        startVmInstance {
            uuid = vm.uuid
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        nic = vm.getVmNics().get(0)
        ip6 = nic.getUsedIps().get(0)
        assert ip6.ip == ip6Str

        nic = attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3.uuid
        }
        UsedIpInventory ip4 = null
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.l3NetworkUuid == l3.uuid) {
                ip4 = ip
            }
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        String ip4Str = (ip4.ip == "192.168.100.11" ? "192.168.100.12" : "192.168.100.11")
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = ip4Str
        }

        startVmInstance {
            uuid = vm.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        nic = vm.getVmNics().get(0)
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.l3NetworkUuid == l3.uuid) {
                assert ip.ip == ip4Str
            } else if (ip.l3NetworkUuid == l3_statefull.uuid) {
                assert ip.ip == ip6Str
            } else {
                assert false
            }
        }
    }

    void testCreateVmWithStaticIpv6() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        String static_ip = "2001:2003--10"
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid]
            defaultL3NetworkUuid = l3_statefull.uuid
            systemTags = [String.format("staticIp::%s::%s", l3_statefull.uuid, static_ip)]
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        VmNicInventory nic = vm.getVmNics().get(0)
        assert nic.usedIps.size() == 1
        UsedIpInventory ip6 = nic.getUsedIps().get(0)
        assert ip6.ip == IPv6NetworkUtils.ipv6TagValueToAddress(static_ip)
        Map<String, String> ips = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert ips.get(l3_statefull.uuid) == IPv6NetworkUtils.ipv6TagValueToAddress(static_ip)

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
        }
        ips = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert ips.get(l3_statefull.uuid) == null

        rebootVmInstance {
            uuid = vm.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        nic = vm.getVmNics().get(0)
        assert nic.usedIps.size() == 1
        ip6 = nic.getUsedIps().get(0)
        assert ip6.ip == IPv6NetworkUtils.ipv6TagValueToAddress(static_ip)
    }

    void testAttachL3NetworkToVmNicWithStaticIpv6() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        VmNicInventory nic = vm.getVmNics().get(0)

        String static_ip = "2001:2003::0110"
        nic = attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3_statefull.uuid
            staticIp = static_ip
        }

        UsedIpInventory ip6 = null
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.l3NetworkUuid == l3_statefull.uuid) {
                ip6 = ip
            }
        }
        assert ip6.ip == "2001:2003::110"
    }
}

