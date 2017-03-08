package org.zstack.test.integration.storage.primary.smp

import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase

/**
 * Created by zouye on 2017/3/1.
 */
class SMPCapacityCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = SMPEnv.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testReleaseSMPCapacityWithNoHostInCase()
        }
    }

    private void testReleaseSMPCapacityWithNoHostInCase() {
        HostSpec hostSpec =  env.specByName("kvm")
        PrimaryStorageSpec primaryStorageSpec = env.specByName("smp")

        deleteHost {
            uuid = hostSpec.inventory.uuid
        }

        PrimaryStorageCapacityVO vo = dbFindByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageCapacityVO.class)
        assert vo.getAvailablePhysicalCapacity() == 0L
        assert vo.getAvailableCapacity() == 0L
        assert vo.getTotalPhysicalCapacity() == 0L
        assert vo.getTotalCapacity() == 0L
        assert vo.getSystemUsedCapacity() == 0L

        PrimaryStorageVO primaryStorageVO = dbFindByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class)
        assert primaryStorageVO.getCapacity().getTotalCapacity() == 0L
        assert primaryStorageVO.getCapacity().getSystemUsedCapacity() == 0L
        assert primaryStorageVO.getCapacity().getAvailableCapacity() == 0L
        assert primaryStorageVO.getCapacity().getTotalPhysicalCapacity() == 0L
        assert primaryStorageVO.getCapacity().getAvailablePhysicalCapacity() == 0L
    }

    @Override
    void clean() {
        env.delete()
    }
}
