package org.zstack.test.integration.kvm.nic

import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.image.ImagePlatform
import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.kvm.vm.OneVmBasicLifeCycleCase
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.core.db.Q

class ChangeWindowsVmNicDriver extends SubCase {
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
            testChangeWindowsVmNicDriver()
        }
    }

    void testChangeWindowsVmNicDriver() {
        VmInstanceInventory vm = env.inventoryByName("vm1") as VmInstanceInventory
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert vm.vmNics[0].driverType = "virtio"

        updateVmInstance {
            platform = ImagePlatform.Windows.toString()
            uuid = vm.uuid
        }
        assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).select(VmNicVO_.driverType).findValue().equals("e1000")

        createSystemTag {
            resourceType = "VmInstanceVo"
            resourceUuid = vm.uuid
            tag = VmSystemTags.VIRTIO.toString()
        }
        assert Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).select(VmNicVO_.driverType).findValue().equals("virtio")
    }
}