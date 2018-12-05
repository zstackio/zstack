package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.compute.vm.VmSystemTags
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.stream.Collectors

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/12/05.
 */
class RecoverDualStackVmInstanceCase extends SubCase {
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
            testCreateDualStackNicFail()
            testCreateDualStackNic()
        }
    }

    void testCreateDualStackNicFail() {
        L3NetworkInventory l3_Stateless_DHCP = env.inventoryByName("l3-Stateless-DHCP")
        L3NetworkInventory l3_Statefull_DHCP = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_Statefull_DHCP_1 = env.inventoryByName("l3-Statefull-DHCP-1")
        L3NetworkInventory l3_vlan_ipv6 = env.inventoryByName("l3-vlan-ipv6")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        /* single nic can not have 2 stateful ipv6 */
        expect(AssertionError.class) {
            createVmInstance {
                name = "vm-1"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = asList(l3_Stateless_DHCP.uuid)
                systemTags = [String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3_Statefull_DHCP.uuid),
                              String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3_Statefull_DHCP_1.uuid)]
            }
        }

        /* single nic can not multiple ipv4 */
        expect(AssertionError.class) {
            createVmInstance {
                name = "vm-2"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = asList(l3_Stateless_DHCP.uuid)
                systemTags = [String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3.uuid),
                              String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3_1.uuid)]
            }
        }

        /* l3 networks of single nic can not be on multiple l2 networks */
        expect(AssertionError.class) {
            createVmInstance {
                name = "vm-3"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = asList(l3_Stateless_DHCP.uuid)
                systemTags = [String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3_Statefull_DHCP.uuid),
                              String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3_vlan_ipv6.uuid)]
            }
        }

        /* single l3 can not be added to vm nic twice */
        expect(AssertionError.class) {
            createVmInstance {
                name = "vm-2"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = asList(l3_Stateless_DHCP.uuid, l3.uuid)
                defaultL3NetworkUuid = l3_Stateless_DHCP.uuid
                systemTags = [String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3.uuid)]
            }
        }

        /* single l3 can not be added to vm nic twice */
        expect(AssertionError.class) {
            createVmInstance {
                name = "vm-2"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = asList(l3_Stateless_DHCP.uuid, l3_Statefull_DHCP_1.uuid)
                defaultL3NetworkUuid = l3_Stateless_DHCP.uuid
                systemTags = [String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3.uuid),
                              String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Statefull_DHCP_1.uuid, l3.uuid)]
            }
        }
    }

    void testCreateDualStackNic() {
        L3NetworkInventory l3_Stateless_DHCP = env.inventoryByName("l3-Stateless-DHCP")
        L3NetworkInventory l3_Statefull_DHCP = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory h1 = env.inventoryByName("kvm-1")
        HostInventory h2 = env.inventoryByName("kvm-2")

        /* create a vm with dual stack, then destroy vm, recover vm, migrate vm */
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_Stateless_DHCP.uuid)
            hostUuid = h1.uuid
            systemTags = [String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3_Statefull_DHCP.uuid),
                          String.format("%s::%s::%s", VmSystemTags.DUAL_STACK_NIC_TOKEN, l3_Stateless_DHCP.uuid, l3.uuid)]
        }
        assert vm.defaultL3NetworkUuid == l3.uuid
        VmNicInventory nic = vm.getVmNics()[0]
        assert nic.l3NetworkUuid == l3.uuid
        assert nic.getUsedIps().size() == 3
        List<String> l3Uuids = nic.getUsedIps().stream().map{ip -> ip.getL3NetworkUuid()}.distinct().collect(Collectors.toList())
        assert l3Uuids.size() == 3
        List<Map<String, String>> tokenList = VmSystemTags.DUAL_STACK_NIC.getTokensOfTagsByResourceUuid(vm.uuid)
        assert tokenList.size() == 2
        Set<String> secondaryL3Uuids = new HashSet()
        for (Map<String, String> tokens : tokenList) {
            String primaryL3Uuid = tokens.get(VmSystemTags.DUAL_STACK_NIC_PRIMARY_L3_TOKEN)
            String secondaryL3Uuid = tokens.get(VmSystemTags.DUAL_STACK_NIC_SECONDARY_L3_TOKEN)
            secondaryL3Uuids.add(secondaryL3Uuid)
            assert primaryL3Uuid == l3.uuid
        }
        assert secondaryL3Uuids.size() == 2
        assert secondaryL3Uuids.contains(l3_Stateless_DHCP.uuid)
        assert secondaryL3Uuids.contains(l3_Statefull_DHCP.uuid)

        destroyVmInstance {
            uuid = vm.uuid
        }

        recoverVmInstance {
            uuid = vm.uuid
        }

        vm = startVmInstance {
            uuid = vm.uuid
            hostUuid = h1.uuid
        }
        assert vm.defaultL3NetworkUuid == l3.uuid
        nic = vm.getVmNics()[0]
        assert nic.l3NetworkUuid == l3.uuid
        assert nic.getUsedIps().size() == 3
        l3Uuids = nic.getUsedIps().stream().map{ip -> ip.getL3NetworkUuid()}.distinct().collect(Collectors.toList())
        assert l3Uuids.size() == 3

        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = h2.uuid
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        }[0]
        assert vm.defaultL3NetworkUuid == l3.uuid
        nic = vm.getVmNics()[0]
        assert nic.l3NetworkUuid == l3.uuid
        assert nic.getUsedIps().size() == 3
        l3Uuids = nic.getUsedIps().stream().map{ip -> ip.getL3NetworkUuid()}.distinct().collect(Collectors.toList())
        assert l3Uuids.size() == 3
    }
}

