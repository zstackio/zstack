package org.zstack.test.integration.storage.primary.ceph.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.SQL
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.sdk.*
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by lining on 2018/11/7.
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
            testCreateDataVolume()
            testCreateVm()
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
            rsp.totalCapacity = beforePsCapacity.totalCapacity + addSize
            rsp.availableCapacity = beforePsCapacity.availableCapacity + addSize
            rsp.poolCapacities = [
                    new CephPoolCapacity(
                            name : primaryStoragePool.poolName,
                            usedCapacity: primaryStoragePool.usedCapacity,
                            availableCapacity : primaryStoragePool.availableCapacity + addSize,
                            totalCapacity: primaryStoragePool.totalCapacity + addSize
                    ),
                    new CephPoolCapacity(
                            name : "other-pool",
                            availableCapacity : 10,
                            usedCapacity: 10,
                            totalCapacity: 20,
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
        retryInSecs {
            afterPsCapacity = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }
            assert addSize == afterPsCapacity.availableCapacity - beforePsCapacity.availableCapacity
        }

        CephPrimaryStoragePoolInventory afterPrimaryStoragePool = queryCephPrimaryStoragePool {}[0]
        assert afterPrimaryStoragePool.availableCapacity - primaryStoragePool.availableCapacity == addSize

        BackupStorageInventory afterBs = queryBackupStorage {
            conditions = ["uuid=${bs.uuid}"]
        }[0]
        assert afterBs.availableCapacity == bs.availableCapacity + addSize
        assert afterBs.totalCapacity == bs.totalCapacity + addSize
    }

    void testCreateDataVolume() {
        PrimaryStorageGlobalConfig.RESERVED_CAPACITY.updateValue(0)

        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        BackupStorageInventory bs = env.inventoryByName("ceph-bk")
        CephPrimaryStoragePoolInventory primaryStoragePool = queryCephPrimaryStoragePool {
            conditions = ["type=Data"]
        }[0]
        CephPrimaryStoragePoolInventory cachePool = queryCephPrimaryStoragePool {
            conditions = ["type=ImageCache"]
        }[0]

        long poolSize = 100

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = poolSize
            rsp.availableCapacity = poolSize
            rsp.poolCapacities = [
                    new CephPoolCapacity(
                            name : primaryStoragePool.poolName,
                            usedCapacity: 0,
                            availableCapacity : poolSize,
                            totalCapacity: poolSize
                    ),
                    new CephPoolCapacity(
                            name : cachePool.poolName,
                            usedCapacity: 0,
                            availableCapacity : poolSize,
                            totalCapacity: poolSize
                    )
            ]
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        DiskOfferingInventory diskOffering = createDiskOffering {
            name = "testDiskOffering"
            diskSize = poolSize + 1
        }

        expectError {
            createDataVolume {
                name = "dataVolume"
                primaryStorageUuid = ps.uuid
                diskOfferingUuid = diskOffering.uuid
            }
        }

        diskOffering = createDiskOffering {
            name = "testDiskOffering"
            diskSize = poolSize - 1
        }

        VolumeInventory volumeInventory = createDataVolume {
            name = "dataVolume"
            primaryStorageUuid = ps.uuid
            diskOfferingUuid = diskOffering.uuid
        }

        deleteDataVolume {
            uuid = volumeInventory.uuid
        }

        expungeDataVolume {
            uuid = volumeInventory.uuid
        }
    }

    void testCreateVm() {
        PrimaryStorageGlobalConfig.RESERVED_CAPACITY.updateValue(0)

        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        CephPrimaryStoragePoolInventory rootPool = queryCephPrimaryStoragePool {
            conditions = ["type=Root"]
        }[0]
        CephPrimaryStoragePoolInventory cachePool = queryCephPrimaryStoragePool {
            conditions = ["type=ImageCache"]
        }[0]

        long poolSize = 100

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = poolSize
            rsp.availableCapacity = poolSize
            rsp.poolCapacities = [
                    new CephPoolCapacity(
                            name : rootPool.poolName,
                            usedCapacity: 0,
                            availableCapacity : poolSize,
                            totalCapacity: poolSize
                    ),
                    new CephPoolCapacity(
                            name : cachePool.poolName,
                            usedCapacity: 0,
                            availableCapacity : poolSize,
                            totalCapacity: poolSize
                    )
            ]
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        VmInstanceInventory testVm = env.inventoryByName("test-vm")
        SQL.New(ImageVO.class)
                .eq(ImageVO_.uuid, testVm.imageUuid)
                .set(ImageVO_.size, poolSize + 1)
                .set(ImageVO_.actualSize, poolSize + 1)
                .update()

        expect(AssertionError.class){
            createVmInstance {
                name = "vm"
                instanceOfferingUuid = testVm.instanceOfferingUuid
                imageUuid = testVm.imageUuid
                l3NetworkUuids = [testVm.defaultL3NetworkUuid]
            }
        }

        SQL.New(ImageVO.class)
                .eq(ImageVO_.uuid, testVm.imageUuid)
                .set(ImageVO_.size, poolSize - 1)
                .set(ImageVO_.actualSize, poolSize - 1)
                .update()

        createVmInstance {
            name = "vm"
            instanceOfferingUuid = testVm.instanceOfferingUuid
            imageUuid = testVm.imageUuid
            l3NetworkUuids = [testVm.defaultL3NetworkUuid]
        }
    }
}
