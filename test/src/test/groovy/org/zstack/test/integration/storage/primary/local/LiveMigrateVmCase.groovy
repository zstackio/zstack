package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by miao on 17-5-7.
 */
class LiveMigrateVmCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

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
            LocalStoragePrimaryStorageGlobalConfig.ALLOW_LIVE_MIGRATION.updateValue(Boolean.TRUE.toString())
            testLiveMigrateVmWithDataVolume()
        }
    }

    void testLiveMigrateVmWithDataVolume() {
        dbf = bean(DatabaseFacade.class)
        VmInstanceInventory vm1 = (VmInstanceInventory) env.inventoryByName("vm")
        def invs = queryHost {
        } as List<HostInventory>
        def targetHostUuid = invs.find { i -> i.uuid != vm1.getHostUuid() }.getUuid()

        migrateVm {
            vmInstanceUuid = vm1.getUuid()
            hostUuid = targetHostUuid
        }

        retryInSecs {
            VmInstanceVO vmInstanceVO = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class)
            assert vmInstanceVO.hostUuid == targetHostUuid
        }
    }
}
