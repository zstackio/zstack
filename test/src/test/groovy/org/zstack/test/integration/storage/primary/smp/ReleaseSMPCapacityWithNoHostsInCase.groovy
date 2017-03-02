package org.zstack.test.integration.storage.primary.smp

import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase

/**
 * Created by zouye on 2017/3/1.
 */
class ReleaseSMPCapacityWithNoHostsInCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            smp()
            virtualRouter()
            vyos()
            kvm()
        }
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
        PrimaryStorageInventory inv = env.specByName("smp")

        deleteHost {
            uuid = hostSpec.inventory.uuid
        }

        PrimaryStorageCapacityVO vo = dbFindByUuid(inv.uuid, PrimaryStorageCapacityVO.class)

        assert vo.getAvailablePhysicalCapacity() == 0L
        assert vo.getAvailableCapacity() == 0L
        assert vo.getTotalPhysicalCapacity() == 0L
        assert vo.getTotalCapacity() == 0L
        assert vo.getSystemUsedCapacity() == 0L
    }

    @Override
    void clean() {
        env.delete()
    }
}
