package org.zstack.test.integration.storage.primary.ceph.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CephBackupStorageInventory
import org.zstack.sdk.CephPrimaryStoragePoolInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ReconnectPrimaryStorageAction
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.CephBackupStorageSpec
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collector
import java.util.stream.Collectors

class CephOpenSourcePoolCapacityCase extends SubCase {
    EnvSpec env
    boolean overflowManagedPool

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
        testCapacityRange()
        env.hijackSimulator(CephPrimaryStorageBase.INIT_PATH) { rsp, HttpEntity<String> e ->
            Map response = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(rsp), LinkedHashMap.class)
            if (overflowManagedPool) {
                response.poolCapacities[0].availableCapacity = new BigInteger("20496382256601145344")
            }
            response.poolCapacities.add([
                    name: "unmanaged-object-index", relatedOsds: "osd.unmanaged",
                    availableCapacity: new BigInteger("20496382256601145344"),
                    totalCapacity: new BigInteger("20496382256601145344"), usedCapacity: 0L
            ])
            return response
        }
        env.create {
            testReconnectPrimaryStorage()
        }
    }

    void testReconnectPrimaryStorage() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        CephBackupStorageInventory bs = env.inventoryByName("ceph-bk")
        def existingPools = queryCephPrimaryStoragePool {
            conditions=["primaryStorageUuid=${ps.uuid}".toString()]
        } as List<CephPrimaryStoragePoolInventory>
        existingPools = existingPools.stream().map {t -> t.getPoolName()}.collect(Collectors.toList())
        assert existingPools.size() == 3

        def otherPools = ["pool-1", "pool-2", "pool-3"]
        def returnPools = otherPools

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
                            name : returnPools.get(0),
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity : osdMap,
                            diskUtilization: 0.33
                    ),
                    new CephPoolCapacity(
                            name : returnPools.get(1),
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity: osdMap,
                            diskUtilization: 0.33
                    ),
                    new CephPoolCapacity(
                            name : returnPools.get(2),
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
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity: osdMap,
                            diskUtilization: 0.33
                    )
            ]
            rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
            return rsp
        }
        env.simulator(CephBackupStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.InitCmd.class)
            CephBackupStorageSpec cspec = spec.specByUuid(cmd.uuid)

            Map<String, CephPoolCapacity.OsdCapacity> osdMap = new HashMap<>()
            osdMap.put("osd.0", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))
            osdMap.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))
            osdMap.put("osd.2", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))
            def rsp = new CephBackupStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.totalCapacity = 1000
            rsp.availableCapacity = 999
            rsp.poolCapacities = [
                    new CephPoolCapacity(
                            name : returnPools.get(0),
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity : osdMap,
                            diskUtilization: 0.33
                    ),
                    new CephPoolCapacity(
                            name : returnPools.get(1),
                            usedCapacity: SizeUnit.GIGABYTE.toByte(10),
                            availableCapacity : SizeUnit.GIGABYTE.toByte(90),
                            totalCapacity: SizeUnit.GIGABYTE.toByte(100),
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity: osdMap,
                            diskUtilization: 0.33
                    ),
                    new CephPoolCapacity(
                            name : returnPools.get(2),
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
                            relatedOsds: "osd.1,osd.2,osd.3",
                            relatedOsdCapacity: osdMap,
                            diskUtilization: 0.33
                    )
            ]
            rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
            return rsp
        }

        returnPools = otherPools
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult afterPsCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert 999L == afterPsCapacity.availablePhysicalCapacity
        assert 1000L == afterPsCapacity.totalCapacity
        assert 1000L == afterPsCapacity.totalPhysicalCapacity

        returnPools = existingPools
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        GetPrimaryStorageCapacityResult psCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert psCapacity.availablePhysicalCapacity == 95670403072 // ~=89.1G
        assert psCapacity.totalCapacity == 106300440576 // 99G
        assert psCapacity.totalPhysicalCapacity == 106300440576

        reconnectBackupStorage {
            uuid = bs.uuid
        }

        psCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert psCapacity.availablePhysicalCapacity == 95670403072 // ~=89.1G
        assert psCapacity.totalCapacity == 106300440576 // 99G
        assert psCapacity.totalPhysicalCapacity == 106300440576

        BackupStorageInventory bsCapacity = queryBackupStorage {
            conditions = ["uuid=${bs.uuid}"]
        }[0]
        assert bsCapacity.availableCapacity == 95670403072 // ~=89.1G
        assert bsCapacity.totalCapacity == 106300440576 // 99G

        overflowManagedPool = true
        def action = new ReconnectPrimaryStorageAction()
        action.uuid = ps.uuid
        action.sessionId = adminSession()
        def result = action.call()
        assert result.error != null: "Managed pool overflow must not become a truncated capacity"
        assert result.error.code != "sdk.1001": "Managed pool overflow must return an error without timing out"
        assert JSONObjectUtil.toJsonString(result.error).contains("BigInteger out of long range")
        overflowManagedPool = false
        reconnectPrimaryStorage { uuid = ps.uuid }

        env.cleanSimulatorAndMessageHandlers()
    }

    void testCapacityRange() {
        ["AvailableCapacity", "UsedCapacity", "TotalCapacity"].each { suffix ->
            String field = suffix.substring(0, 1).toLowerCase() + suffix.substring(1)
            [null, Long.MIN_VALUE, 0L, Long.MAX_VALUE].each { value ->
                def pool = new CephPoolCapacity()
                pool."set${suffix}"(value as Long)
                String json = JSONObjectUtil.toJsonString(pool)
                def restored = JSONObjectUtil.toObject(json, CephPoolCapacity.class)
                assert restored."get${suffix}"() == value
            }
            [new BigInteger("20496382256601145344"),
             BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),
             BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE)].each { value ->
                def pool = JSONObjectUtil.toObject("{\"${field}\":${value}}", CephPoolCapacity.class)
                assert JSONObjectUtil.toJsonString(pool).contains(value.toString()): "Deserialization must retain the full integer"
                boolean overflow = false
                try {
                    pool."get${suffix}"()
                } catch (ArithmeticException ignored) {
                    overflow = true
                }
                assert overflow: "Reading out-of-range ${field} must fail instead of truncating ${value}"
            }
        }
    }
}
