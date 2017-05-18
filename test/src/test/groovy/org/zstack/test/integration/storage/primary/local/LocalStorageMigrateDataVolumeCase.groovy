package org.zstack.test.integration.storage.primary.local

import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase

class LocalStorageMigrateDataVolumeCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmWithOneDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            testLocalStorageMigrateDataVolume()
        }
    }

    void testLocalStorageMigrateDataVolume() {
        HostSpec hostSpec1 = (HostSpec) env.specByName("kvm1")
        VmInstanceInventory vm1 = (VmInstanceInventory) env.inventoryByName("vm")
        VolumeInventory dataVolume = vm1.getAllVolumes().find { i -> i.getUuid() != vm1.getRootVolumeUuid() }
        detachDataVolumeFromVm {
            uuid = dataVolume.getUuid()
            vmUuid = vm1.getUuid()
        }

        boolean calledCheckMD5 = false
        env.afterSimulator(LocalStorageKvmBackend.CHECK_MD5_PATH) { rsp ->
            calledCheckMD5 = true
            return rsp
        }

        boolean calledCopyToRemote = false
        env.afterSimulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) { rsp ->
            calledCopyToRemote = true
            return rsp
        }


        localStorageMigrateVolume {
            volumeUuid = dataVolume.getUuid()
            destHostUuid = hostSpec1.inventory.uuid
        }

        retryInSecs(6) {
            assert calledCheckMD5 && calledCopyToRemote
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
