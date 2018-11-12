package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by shixin on 2018/11/12.
 */
class StaticIpLeakWhenMacDupicatedCase extends SubCase {
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
            testIpAddressLeak()
            testIpAddressLeakWhenAttachNic()
        }
    }

    void testIpAddressLeak() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        String staticMac = "00:00:01:00:00:02"
        String staticIp = "192.168.100.10"

        VmInstanceInventory vm = createVmInstance {
            name = "vm-static-ip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
            systemTags = [String.format("%s::%s::%s", VmSystemTags.STATIC_IP_TOKEN, l3.uuid, staticIp),
                          String.format("%s::%s::%s", VmSystemTags.MAC_TOKEN, l3.uuid, staticMac)]
        }
        assert vm.getVmNics().size() == 1

        VmNicInventory nic = vm.getVmNics().get(0)
        assert nic.mac == staticMac
        assert nic.ip == staticIp

        destroyVmInstance {
            uuid = vm.uuid
        }
        vm = queryVmInstance {conditions=["uuid=${vm.uuid}".toString()]}[0]
        nic = vm.getVmNics().get(0)
        assert nic.mac == staticMac
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).eq(UsedIpVO_.ip, staticIp).isExists()

        expect (AssertionError.class) {
            VmInstanceInventory vm1 = createVmInstance {
                name = "vm-static-ip"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                defaultL3NetworkUuid = l3.uuid
                systemTags = [String.format("%s::%s::%s", VmSystemTags.STATIC_IP_TOKEN, l3.uuid, staticIp),
                              String.format("%s::%s::%s", VmSystemTags.MAC_TOKEN, l3.uuid, staticMac)]
            }
        }
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).eq(UsedIpVO_.ip, staticIp).isExists()
    }

    void testIpAddressLeakWhenAttachNic() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_200_6 = env.inventoryByName("l3-vlan-ipv6")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-static-ip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }
        assert vm.getVmNics().size() == 1
        VmNicInventory nic = vm.getVmNics().get(0)

        /* reserve the ip */
        String staticIp6 = "3001:2001::2"
        createVip {
            name = "vip6"
            l3NetworkUuid = l3_200_6.uuid
            requiredIp = staticIp6
        }

        expect (AssertionError.class) {
            attachL3NetworkToVm {
                l3NetworkUuid = l3_200_6.uuid
                vmInstanceUuid = vm.uuid
                staticIp = staticIp6
            }
        }
        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).eq(UsedIpVO_.ip, nic.ip).isExists()
        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3_200_6.uuid).eq(UsedIpVO_.ip, staticIp6).isExists()
        assert Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.uuid).isExists()
    }
}

