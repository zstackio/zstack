package org.zstack.test.integration.storage.primary.local.datavolume

import org.zstack.core.db.DatabaseFacade
import org.zstack.sdk.LocalStorageMigrateVolumeAction
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*

/**
 * Created by camile on 2017/5/4.
 */
class MigrateVolumenCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void test() {
        env.create {
            testMigrateVolumeWhenPsIsMaintainFailure()
        }
    }


    void testMigrateVolumeWhenPsIsMaintainFailure() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec = env.specByName("test-vm")
        KVMHostSpec kvm = env.specByName("kvm")
        KVMHostSpec kvm1 = env.specByName("kvm1")
        DiskOfferingSpec disk = env.specByName("diskOffering")
        String vmUuid = vmSpec.inventory.uuid
        String imageUuid = (env.specByName("test-iso") as ImageSpec).inventory.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        VolumeInventory dataVolume = createDataVolume{
            name = "1G"
            diskOfferingUuid = disk.inventory.uuid
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            systemTags =Arrays.asList("localStorage::hostUuid::"+kvm.inventory.uuid)
        }
        changePrimaryStorageState{
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = "maintain"
        }
        LocalStorageMigrateVolumeAction localStorageMigrateVolumeAction= new  LocalStorageMigrateVolumeAction()
        localStorageMigrateVolumeAction.volumeUuid = dataVolume.uuid
        localStorageMigrateVolumeAction.destHostUuid = kvm1.inventory.uuid
        localStorageMigrateVolumeAction.sessionId = adminSession()
        LocalStorageMigrateVolumeAction.Result res = localStorageMigrateVolumeAction.call()
        res.error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
