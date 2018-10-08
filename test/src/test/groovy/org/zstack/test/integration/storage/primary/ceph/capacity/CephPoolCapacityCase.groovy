package org.zstack.test.integration.storage.primary.ceph.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by SyZhao on 2017/4/17.
 */
class CephPoolCapacityCase extends SubCase {
    EnvSpec env

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
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testReconnectPrimaryStorage()
        }
    }

    void testReconnectPrimaryStorage() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        BackupStorageInventory bs = env.inventoryByName("ceph-bk")

        CephPrimaryStoragePoolInventory primaryStoragePool = queryCephPrimaryStoragePool {}[0]

        GetPrimaryStorageCapacityResult beforePsCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        long addSize = 1

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = 1000
            rsp.availableCapacity = 999
            rsp.poolCapacities = [
                    new CephPoolCapacity(
                            name : primaryStoragePool.poolName,
                            availableCapacity : primaryStoragePool.availableCapacity + addSize,
                            usedCapacity: primaryStoragePool.usedCapacity
                    ),
                    new CephPoolCapacity(
                            name : "other-pool",
                            availableCapacity : 10,
                            usedCapacity: 10
                    ),
                    new CephPoolCapacity(
                            availableCapacity : 11,
                            usedCapacity: 11
                    )
            ]
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult afterPsCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert afterPsCapacity.availablePhysicalCapacity - beforePsCapacity.availablePhysicalCapacity == addSize
        assert afterPsCapacity.totalCapacity - beforePsCapacity.availablePhysicalCapacity == addSize
        assert afterPsCapacity.totalPhysicalCapacity - beforePsCapacity.totalPhysicalCapacity == addSize

        CephPrimaryStoragePoolInventory afterPrimaryStoragePool = queryCephPrimaryStoragePool {}[0]
        assert afterPrimaryStoragePool.availableCapacity - primaryStoragePool.availableCapacity == addSize

        BackupStorageInventory afterBs = queryBackupStorage {
            conditions = ["uuid=${bs.uuid}"]
        }[0]
        assert afterBs.availableCapacity == bs.availableCapacity
        assert afterBs.totalCapacity == bs.totalCapacity
    }
}
