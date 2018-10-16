package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

import java.util.stream.Collectors

import static java.util.Arrays.asList

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
        }
    }

    void testCreateVmOfPrivateIPv6Network() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_statefull_1 = env.inventoryByName("l3-Statefull-DHCP-1")
        L3NetworkInventory l3_stateless = env.inventoryByName("l3-Stateless-DHCP")
        L3NetworkInventory l3_slaac = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
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
}

