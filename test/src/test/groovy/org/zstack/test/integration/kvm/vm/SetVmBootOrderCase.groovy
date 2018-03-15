package org.zstack.test.integration.kvm.vm

import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.vm.VmBootDevice
import org.zstack.sdk.GetVmBootOrderResult
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static java.util.Arrays.asList

class SetVmBootOrderCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testSetVmBootFromHardDisk()
            testSetVmBootFromCdRom()
            testSetVmBootFromCdRomOnce()
            testSetVmBootFromCdRomOnceTemporarily()
        }
    }

    void testSetVmBootFromHardDisk() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString())
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert res.orders.get(1) == VmBootDevice.CdRom.toString()
    }

    void testSetVmBootFromCdRom() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
    }

    void testSetVmBootFromCdRomOnce() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
            systemTags = ["cdromBootOnce::true"]
        }

        // before vm reboot
        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert "true" == VmSystemTags.CDROM_BOOT_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.CDROM_BOOT_ONCE_TOKEN)

        rebootVmInstance {
            uuid = vm.uuid
        }

        // after vm reboot
        res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 1
        assert res.orders.get(0) == VmBootDevice.HardDisk.toString()
        assert null == VmSystemTags.CDROM_BOOT_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.CDROM_BOOT_ONCE_TOKEN)
    }

    void testSetVmBootFromCdRomOnceTemporarily() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        // set boot from cdrom once only
        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
            systemTags = ["cdromBootOnce::true"]
        }

        // regret
        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString())
        }

        // before vm reboot
        GetVmBootOrderResult res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert null == VmSystemTags.CDROM_BOOT_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.CDROM_BOOT_ONCE_TOKEN)

        rebootVmInstance {
            uuid = vm.uuid
        }

        // after vm reboot
        res = getVmBootOrder {
            uuid = vm.uuid
        }
        assert res.orders.size() == 2
        assert res.orders.get(0) == VmBootDevice.CdRom.toString()
        assert res.orders.get(1) == VmBootDevice.HardDisk.toString()
        assert null == VmSystemTags.CDROM_BOOT_ONCE.getTokenByResourceUuid(vm.uuid, VmSystemTags.CDROM_BOOT_ONCE_TOKEN)
    }
}
