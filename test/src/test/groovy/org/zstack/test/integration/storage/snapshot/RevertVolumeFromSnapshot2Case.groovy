package org.zstack.test.integration.storage.snapshot

import org.zstack.core.db.Q
import org.zstack.header.core.trash.InstallPathRecycleVO
import org.zstack.header.core.trash.InstallPathRecycleVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageResourceRefVO
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class RevertVolumeFromSnapshot2Case extends SubCase{
    EnvSpec env
    VmInstanceInventory vm
    VolumeInventory root

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory

            testRevertRecover()
        }
    }

    void testRevertRecover() {
        String vUuid = vm.rootVolumeUuid

        def snapshot = createVolumeSnapshot {
            name = "1.0"
            volumeUuid = vUuid
        }

        def snapshot1 = createVolumeSnapshot {
            name = "1.1"
            volumeUuid = vUuid
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        def currentInstallPath = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vUuid).select(VolumeVO_.installPath).findValue()
        assert !Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.installPath, currentInstallPath).isExists()

        revertVolumeFromSnapshot {
            uuid = snapshot.uuid
        }

        assert Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.installPath, currentInstallPath).isExists()

        startVmInstance {
            uuid = vm.uuid
        }

        assert Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()

        batchDeleteVolumeSnapshot {
            uuids = [snapshot.uuid, snapshot1.uuid]
        }

        assert Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()

        destroyVmInstance {
            uuid = vm.uuid
        }

        assert Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()

        recoverVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        expungeVmInstance {
            uuid = vm.uuid
        }

        assert !Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()
    }
}

