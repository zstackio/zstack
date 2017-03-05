package org.zstack.test.integration.storage

import org.zstack.test.integration.storage.primary.local.LocalStorageHostRefVOCase
import org.zstack.test.integration.storage.primary.local.LocalStorageMigrateVolumeCase
import org.zstack.test.integration.storage.primary.smp.SMPCapacityCase
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/27.
 */
class StorageTest extends Test {
    static SpringSpec springSpec = makeSpring {
        localStorage()
        nfsPrimaryStorage()
        sftpBackupStorage()
        smp()
        ceph()
        virtualRouter()
        vyos()
        kvm()
        securityGroup()
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases([
                new LocalStorageMigrateVolumeCase(),
                new SMPCapacityCase(),
                new LocalStorageHostRefVOCase(),
        ])
    }
}
