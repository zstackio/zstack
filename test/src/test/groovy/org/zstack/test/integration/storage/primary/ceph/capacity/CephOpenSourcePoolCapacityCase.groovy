package org.zstack.test.integration.storage.primary.ceph.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CephBackupStorageInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class CephOpenSourcePoolCapacityCase extends SubCase {
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
        CephBackupStorageInventory bs = env.inventoryByName("ceph-bk")

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)

            Map<String, CephPoolCapacity.OsdCapacity> osdMap = new HashMap<>()
            osdMap.put("osd.0", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))
            osdMap.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))
            osdMap.put("osd.2", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))
            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = 1000
            rsp.availableCapacity = 999
            rsp.poolCapacities = [
                    new CephPoolCapacity(
                            name : "test-pool1",
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity : osdMap,
                            diskUtilization: 0.33
                    ),
                    new CephPoolCapacity(
                            name : "test-pool2",
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity: osdMap,
                            diskUtilization: 0.33
                    ),
                    new CephPoolCapacity(
                            name : "test-pool3",
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity: osdMap,
                            diskUtilization: 0.33
                    ),
                    new CephPoolCapacity(
                            name : bs.poolName,
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.4,osd.5,osd.6"
                    )
            ]
            rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
            return rsp
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult psCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert psCapacity.availablePhysicalCapacity == 95670403072 // ~=89.1G
        assert psCapacity.totalCapacity == 106300440576 // 99G
        assert psCapacity.totalPhysicalCapacity == 106300440576


        BackupStorageInventory bsCapacity = queryBackupStorage {
            conditions = ["uuid=${bs.uuid}"]
        }[0]
        assert bsCapacity.availableCapacity == SizeUnit.GIGABYTE.toByte(90)
        assert bsCapacity.totalCapacity == SizeUnit.GIGABYTE.toByte(100)
    }
}