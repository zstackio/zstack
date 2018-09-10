package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.stream.Collectors

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/10.
 */
class AddIpv6RangeToVmNicsCase extends SubCase {
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
            testAttachDetachL3ToVmNic()
        }
    }

    void testAttachDetachL3ToVmNic() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_statefull_1 = env.inventoryByName("l3-Statefull-DHCP-1")
        L3NetworkInventory l3_stateless = env.inventoryByName("l3-Stateless-DHCP")
        L3NetworkInventory l3_slaac = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        L3NetworkInventory l3_vlan_ipv4 = env.inventoryByName("l3-vlan-ipv4")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }
        VmNicInventory nic = vm.getVmNics()[0]
        List<L3NetworkInventory> l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        List<String> l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert !l3s.contains(l3_statefull.uuid)
        assert l3s.contains(l3_statefull_1.uuid)
        assert l3s.contains(l3.uuid)

        expect(AssertionError.class) {
            attachL3NetworkToVmNic {
                vmNicUuid = nic.uuid
                l3NetworkUuid = l3_statefull.uuid
            }
        }

        expect(AssertionError.class) {
            attachL3NetworkToVmNic {
                vmNicUuid = nic.uuid
                l3NetworkUuid = l3_vlan_ipv4.uuid
            }
        }

        sleep(1)

        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3_stateless.uuid
        }
        l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert !l3s.contains(l3_statefull.uuid)
        assert !l3s.contains(l3_stateless.uuid)
        assert l3s.contains(l3.uuid)

        sleep(1)

        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3_slaac.uuid
        }

        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3.uuid
        }
        l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert !l3s.contains(l3_statefull.uuid)
        assert !l3s.contains(l3_stateless.uuid)
        assert !l3s.contains(l3.uuid)

        expect(AssertionError.class) {
            attachL3NetworkToVmNic {
                vmNicUuid = nic.uuid
                l3NetworkUuid = l3_1.uuid
            }
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        nic = vm.getVmNics()[0]
        assert nic.getUsedIps().size() == 4
        assert nic.l3NetworkUuid == l3.uuid

        UsedIpVO ipVO1 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO1.uuid
        }
        l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert !l3s.contains(l3_statefull.uuid)
        assert !l3s.contains(l3_stateless.uuid)
        assert l3s.contains(l3.uuid)

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        nic = vm.getVmNics()[0]
        assert nic.getUsedIps().size() == 3

        UsedIpVO ipVO2 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3_statefull.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO2.uuid
        }
        l3Invs = getVmAttachableL3Network {
            vmInstanceUuid = vm.uuid
        }
        l3s = l3Invs.stream().map{l3n -> l3n.getUuid()}.collect(Collectors.toList())
        assert l3s.contains(l3_statefull.uuid)
        assert !l3s.contains(l3_stateless.uuid)
        assert l3s.contains(l3.uuid)

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        nic = vm.getVmNics()[0]
        assert nic.getUsedIps().size() == 2

        UsedIpVO ipVO3 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3_stateless.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO3.uuid
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        nic = vm.getVmNics()[0]
        assert nic.getUsedIps().size() == 1

        UsedIpVO ipVO4 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3_slaac.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO4.uuid
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        assert vm.getVmNics().size() == 0
    }


}

