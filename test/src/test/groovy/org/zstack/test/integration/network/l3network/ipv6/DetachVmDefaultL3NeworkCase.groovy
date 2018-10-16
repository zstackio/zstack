package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/10/16.
 */
class DetachVmDefaultL3NeworkCase extends SubCase {
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
            testDetachVmDefaultL3Nework()
        }
    }

    void testDetachVmDefaultL3Nework() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }
        VmNicInventory nic = vm.getVmNics()[0]

        /* test delete l3 network */
        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        assert vm.defaultL3NetworkUuid == l3.uuid

        deleteL3Network {
            uuid = l3.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        assert vm.defaultL3NetworkUuid == l3_statefull.uuid

        /* test delete iprange */
        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3_1.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        assert vm.defaultL3NetworkUuid == l3_1.uuid

        IpRangeInventory ipr = l3_1.getIpRanges().get(0)
        deleteIpRange {
            uuid = ipr.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        assert vm.defaultL3NetworkUuid == l3_statefull.uuid
    }


}

