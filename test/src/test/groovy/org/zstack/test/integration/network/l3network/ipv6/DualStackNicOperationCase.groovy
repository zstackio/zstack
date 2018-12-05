package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.compute.vm.DualStackNicSecondaryNetworksOperator
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.stream.Collectors

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/12/06.
 */
class DualStackNicOperationCase extends SubCase {
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
            testSystemTagsAfterAttachDetachL3()
        }
    }

    void testSystemTagsAfterAttachDetachL3() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_slaac_1 = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }
        VmNicInventory nic = vm.getVmNics()[0]
        nic = attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3.uuid
        }

        assert nic.l3NetworkUuid == l3.uuid
        List<String> secondaryNetworks = new DualStackNicSecondaryNetworksOperator().getSecondaryNetworksByVmUuidNic(vm.uuid, nic.l3NetworkUuid)
        assert secondaryNetworks.size() == 1
        assert secondaryNetworks.contains(l3_statefull.uuid)

        nic = attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3_slaac_1.uuid
        }

        assert nic.l3NetworkUuid == l3.uuid
        secondaryNetworks = new DualStackNicSecondaryNetworksOperator().getSecondaryNetworksByVmUuidNic(vm.uuid, nic.l3NetworkUuid)
        assert secondaryNetworks.size() == 2
        assert secondaryNetworks.contains(l3_statefull.uuid)
        assert secondaryNetworks.contains(l3_slaac_1.uuid)

        destroyVmInstance {
            uuid = vm.uuid
        }

        recoverVmInstance {
            uuid = vm.uuid
        }

        vm = startVmInstance {
            uuid = vm.uuid
        }
        assert vm.defaultL3NetworkUuid == l3.uuid
        nic = vm.getVmNics()[0]
        assert nic.l3NetworkUuid == l3.uuid
        assert nic.getUsedIps().size() == 3
        List<String> l3Uuids = nic.getUsedIps().stream().map{ip -> ip.getL3NetworkUuid()}.distinct().collect(Collectors.toList())
        assert l3Uuids.size() == 3

        detachL3NetworkFromVm {
            vmNicUuid = nic.uuid
        }
        secondaryNetworks = new DualStackNicSecondaryNetworksOperator().getSecondaryNetworksByVmUuidNic(vm.uuid, nic.l3NetworkUuid)
        assert secondaryNetworks == null
    }
}

