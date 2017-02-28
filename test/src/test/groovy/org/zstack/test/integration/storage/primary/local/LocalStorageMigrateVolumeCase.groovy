package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.sdk.LocalStorageMigrateVolumeAction
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.VmSpec

/**
 * Created by zouye on 2017/2/28.
 */
class LocalStorageMigrateVolumeCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorateOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testLocalStorageMigrateVolumeWhenDisable()
            testLocalStorageMigrateVolume()
        }
    }


    void testLocalStorageMigrateVolumeWhenDisable() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec = env.specByName("vm")
        HostSpec hostSpec = env.specByName("kvm")
        HostSpec hostSpec1 = env.specByName("kvm1")
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        assert vmSpec.inventory.rootVolumeUuid
        assert hostSpec.inventory.uuid

        PrimaryStorageVO vo = dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class)
        vo.state = PrimaryStorageState.Disabled
        dbf.updateAndRefresh(vo)

        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Disabled

        stopVmInstance {
            uuid = vmSpec.inventory.uuid
        }

        LocalStorageMigrateVolumeAction action = new LocalStorageMigrateVolumeAction()
        action.volumeUuid = vmSpec.inventory.rootVolumeUuid
        action.destHostUuid = hostSpec1.inventory.uuid
        action.sessionId = Test.currentEnvSpec.session.uuid

        LocalStorageMigrateVolumeAction.Result res = action.call()
        assert res.error != null

        // confirm migrate could be execute when ps is enable
        vo.state = PrimaryStorageState.Enabled
        dbf.updateAndRefresh(vo)
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Enabled
        localStorageMigrateVolume {
            volumeUuid = vmSpec.inventory.rootVolumeUuid
            destHostUuid = hostSpec1.inventory.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
