package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import java.util.stream.Collectors

/**
 * Created by shixin on 2018/09/10.
 */
class AttachIpv6RangeToVmCase extends SubCase {
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
            testAttachDualStackNetworkToVm()
        }
    }

    void testCreateVmOfPrivateIPv6Network() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_statefull_1 = env.inventoryByName("l3-Statefull-DHCP-1")
        L3NetworkInventory l3_stateless = env.inventoryByName("l3-Stateless-DHCP")
        L3NetworkInventory l3_slaac = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
            defaultL3NetworkUuid = l3_statefull.uuid
        }

        List<L3NetworkInventory> l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        List<String> l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert !l3s.contains(l3_statefull.uuid)
        assert l3s.contains(l3_statefull_1.uuid)
        assert !l3s.contains(l3.uuid)
        assert l3s.contains(l3_1.uuid)

        /* create eip for vm */
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3_1.uuid
        }

        VmNicInventory nic = vm.vmNics.stream().filter{nic -> nic.l3NetworkUuid == l3.uuid}.collect(Collectors.toList())[0]
        createEip {
            name = "eip"
            vipUuid = vip.uuid
            vmNicUuid = nic.uuid
        }
        l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert !l3s.contains(l3_1.uuid)

        expect(AssertionError.class) {
            attachL3NetworkToVm {
                l3NetworkUuid = l3_statefull.uuid
                vmInstanceUuid = vm.uuid
            }
        }

        attachL3NetworkToVm {
            l3NetworkUuid = l3_statefull_1.uuid
            vmInstanceUuid = vm.uuid
        }

        l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert !l3s.contains(l3_statefull.uuid)
        assert !l3s.contains(l3_statefull_1.uuid)
        assert !l3s.contains(l3.uuid)

        attachL3NetworkToVm {
            l3NetworkUuid = l3_stateless.uuid
            vmInstanceUuid = vm.uuid
        }

        attachL3NetworkToVm {
            l3NetworkUuid = l3_slaac.uuid
            vmInstanceUuid = vm.uuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        VmInstanceInventory vm1 = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        assert vm1.getVmNics().size() == 5
    }

    void testAttachDualStackNetworkToVm() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-test"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            defaultL3NetworkUuid = l3.uuid
            l3NetworkUuids = [l3.uuid]
        }

        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }

        attachL3NetworkToVm {
            vmInstanceUuid=vm.uuid
            l3NetworkUuid=l3_statefull.uuid
            staticIp="2001:2003::6"
        }
    }
}

