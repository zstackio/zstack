package org.zstack.test.integration.storage.primary.ceph.xsky.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CephBackupStorageInventory
import org.zstack.sdk.CephPrimaryStoragePoolInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.ceph.CephCapacity
import org.zstack.storage.ceph.CephCapacityUpdater
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.primary.PrimaryStorageCapacityRecalculator
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO_
import org.zstack.storage.ceph.primary.capacity.EnterpriseCephPrimaryCapacityBaseUpdater
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
class CephXskyPoolCapacityCase extends SubCase {
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
            testSkipCalculateCapacityWhichInstallPathIsNull()
            testSnapshotsTotalSizeOnSharedOsds()
        }
    }

    void testReconnectPrimaryStorage() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        CephBackupStorageInventory bs = env.inventoryByName("ceph-bk")

        CephPrimaryStoragePoolInventory dataPool = queryCephPrimaryStoragePool {
            conditions = ["type=Data"]
        }[0]

        CephPrimaryStoragePoolInventory rootPool = queryCephPrimaryStoragePool {
            conditions = ["type=Root"]
        }[0]
        CephPrimaryStoragePoolInventory cachePool = queryCephPrimaryStoragePool {
            conditions = ["type=ImageCache"]
        }[0]
        GetPrimaryStorageCapacityResult beforePsCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        long addSize = SizeUnit.GIGABYTE.toByte(100)

        Map<String, CephPoolCapacity.OsdCapacity> osdMap = new HashMap<>()
        osdMap.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))

        Map<String, CephPoolCapacity.OsdCapacity> osdMap2 = new HashMap<>()
        osdMap2.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(200), SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(300)))

        Map<String, CephPoolCapacity.OsdCapacity> osdMap3 = new HashMap<>()
        osdMap3.put("osd.2", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(200)))
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
                            name : rootPool.poolName,
                            usedCapacity: rootPool.usedCapacity,
                            availableCapacity : rootPool.availableCapacity,
                            totalCapacity: rootPool.totalCapacity,
                            relatedOsds: "osd.1",
                            diskUtilization: 0.5,
                            relatedOsdCapacity: osdMap
                    ),
                    new CephPoolCapacity(
                            name : dataPool.poolName,
                            usedCapacity: dataPool.usedCapacity,
                            availableCapacity : dataPool.availableCapacity,
                            totalCapacity: dataPool.totalCapacity,
                            relatedOsds: "osd.1",
                            diskUtilization: 0.33,
                            relatedOsdCapacity: osdMap2
                    ),
                    new CephPoolCapacity(
                            name : cachePool.poolName,
                            usedCapacity: cachePool.usedCapacity,
                            availableCapacity : cachePool.availableCapacity,
                            totalCapacity: cachePool.totalCapacity,
                            diskUtilization: 0.33,
                            relatedOsds: "osd.2",
                            relatedOsdCapacity: osdMap3
                    ),
                    new CephPoolCapacity(
                            name : bs.poolName,
                            usedCapacity:  bs.getPoolUsedCapacity(),
                            availableCapacity : bs.availableCapacity + addSize,
                            totalCapacity: bs.totalCapacity + addSize,
                            relatedOsds: "osd.2"
                    ),
                    new CephPoolCapacity(
                            name : "other-pool",
                            availableCapacity : 10,
                            usedCapacity: 10,
                            totalCapacity: 20,
                            relatedOsds: "osd.3"
                    )
            ]
            rsp.type = "xsky"
            return rsp
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult beforeCap = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        // 200*0.33 + 100*0.33
        assert beforeCap.totalCapacity == SizeUnit.GIGABYTE.toByte(165)
        assert beforeCap.totalPhysicalCapacity == SizeUnit.GIGABYTE.toByte(165)
        assert beforeCap.availablePhysicalCapacity == SizeUnit.GIGABYTE.toByte(99)

        osdMap2.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(200) + addSize, SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(300) + addSize))

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult afterCap = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert afterCap.availablePhysicalCapacity - beforeCap.availablePhysicalCapacity == addSize * 0.33
        assert afterCap.totalCapacity - beforeCap.totalCapacity == addSize * 0.33
        assert afterCap.totalPhysicalCapacity - beforeCap.totalPhysicalCapacity == addSize * 0.33
        retryInSecs {
            afterCap = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }
            assert addSize * 0.33 == afterCap.availableCapacity - beforeCap.availableCapacity
        }

        BackupStorageInventory afterBs = queryBackupStorage {
            conditions = ["uuid=${bs.uuid}"]
        }[0]
        assert afterBs.availableCapacity == bs.availableCapacity + addSize
        assert afterBs.totalCapacity == bs.totalCapacity + addSize
    }

    void testSnapshotsTotalSizeOnSharedOsds() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        def pools = queryCephPrimaryStoragePool { conditions = ["primaryStorageUuid=" + ps.uuid] }
        Set managedNames = pools*.poolName as Set
        long mib = SizeUnit.MEGABYTE.toByte(1)
        Long snapshotBytes = 0L
        Map<String, List<String>> osdsByPool = [:]
        Map<String, Integer> copiesByPool = [:]
        Map<String, Long> bytesByOsd = [:]
        List<String> externalOsds = ["osd.99"]
        CephPrimaryStorageBase.InitRsp lastReport

        env.afterSimulator(CephPrimaryStorageBase.INIT_PATH) { CephPrimaryStorageBase.InitRsp rsp, HttpEntity<String> request ->
            rsp.poolCapacities.each { CephPoolCapacity pool ->
                boolean managed = managedNames.contains(pool.name)
                def osds = managed ? osdsByPool.getOrDefault(pool.name, ["osd.0", "osd.1", "osd.2"]) : externalOsds
                pool.securityPolicy = "Copy"
                pool.replicatedSize = copiesByPool.getOrDefault(pool.name, 3)
                pool.diskUtilization = 1.0f / pool.replicatedSize
                pool.relatedOsds = osds.join(",")
                pool.relatedOsdCapacity = osds.collectEntries { osd ->
                    def stat = new CephPoolCapacity.OsdCapacity(768 * mib * 1024, 0L, 768 * mib * 1024)
                    stat.snapshotUsedBytes = osd == "osd.99" ? 900 * mib : bytesByOsd.getOrDefault(osd, snapshotBytes)
                    [(osd): stat]
                }
            }
            lastReport = rsp
            return rsp
        }

        def recalculate = { new PrimaryStorageCapacityRecalculator(psUuids: [ps.uuid]).recalculate() }
        def reconnect = {
            reconnectPrimaryStorage { uuid = ps.uuid }
            recalculate()
        }
        def capacity = { getPrimaryStorageCapacity { primaryStorageUuids = [ps.uuid] } }
        def poolCapacity = { queryCephPrimaryStoragePool { conditions = ["primaryStorageUuid=" + ps.uuid] }*.availableCapacity }
        reconnect()
        def baseline = capacity()
        long groupFree = poolCapacity().first()
        def checkBalance = { long charged ->
            retryInSecs {
                def current = capacity()
                assert current.availableCapacity == baseline.availableCapacity - charged :
                        "Shared OSDs must be charged once: expected=${baseline.availableCapacity - charged} actual=${current.availableCapacity}"
                def free = poolCapacity()
                assert free.every { it == groupFree - charged } :
                        "All pools must expose the same group balance: expected=${groupFree - charged} actual=${free}"
                assert current.availablePhysicalCapacity == baseline.availablePhysicalCapacity :
                        "Virtual snapshot accounting changed physical free: expected=${baseline.availablePhysicalCapacity} actual=${current.availablePhysicalCapacity}"
            }
        }

        def checkReport = { String manufacturer, Long sample, long charged ->
            lastReport.poolCapacities.findAll { managedNames.contains(it.name) }.each { pool ->
                pool.relatedOsdCapacity.values().each { it.snapshotUsedBytes = sample }
            }
            def response = new CephPrimaryStorageBase.AgentResponse(type: manufacturer, poolCapacities: lastReport.poolCapacities,
                    totalCapacity: lastReport.totalCapacity, availableCapacity: lastReport.availableCapacity)
            new CephCapacityUpdater().update(new CephCapacity(lastReport.fsid, response))
            checkBalance(charged)
        }

        checkReport("sandstone", 33 * mib, 0L)
        checkReport("xsky", null, 0L)
        checkReport("xsky", 33 * mib, 33 * mib)
        checkReport("xsky", 0L, 33 * mib)

        snapshotBytes = 12 * mib
        reconnectPrimaryStorage { uuid = ps.uuid }
        checkBalance(12 * mib)
        checkReport("xsky", 0L, 12 * mib)
        EnterpriseCephPrimaryCapacityBaseUpdater.recentSnapshotCapacityRecalculations.invalidate(ps.uuid)
        checkReport("xsky", null, 12 * mib)
        checkReport("xsky", 0L, 0L)
        snapshotBytes = 0L

        testBatchRecalculateWithEmptyPrimaryStorage(ps, baseline.availableCapacity)

        def primarySnapshotSize = {
            Q.New(CephPrimaryStorageVO.class).eq(CephPrimaryStorageVO_.uuid, ps.uuid)
                    .select(CephPrimaryStorageVO_.snapshotsTotalSize).findValue()
        }
        pools.eachWithIndex { pool, i ->
            osdsByPool[pool.poolName] = (0..2).collect { "osd." + (3 * i + it) }
            bytesByOsd["osd." + 3 * i] = 2048L
        }
        reconnect()
        assert primarySnapshotSize() == pools.size() * 682L : "Independent pools add their weighted OSD usage"
        copiesByPool[pools[0].poolName] = 2
        reconnect()
        assert primarySnapshotSize() == 1024L + (pools.size() - 1) * 682L

        osdsByPool.clear()
        copiesByPool.clear()
        bytesByOsd.clear()
        osdsByPool[pools[0].poolName] = ["osd.1", "osd.2", "osd.3"]
        reconnect()
        long overlapBaseline = capacity().availableCapacity
        def groupBaseline = poolCapacity().sort()
        snapshotBytes = 12 * mib
        reconnect()
        retryInSecs {
            assert primarySnapshotSize() == 16 * mib : "Count the four distinct OSDs once at three replicas"
            assert capacity().availableCapacity == overlapBaseline - 16 * mib
            assert poolCapacity().sort() == groupBaseline.collect { it - 12 * mib }
        }

        osdsByPool[pools[2].poolName] = ["osd.5", "osd.6", "osd.7"]
        snapshotBytes = 0L
        bytesByOsd["osd.0"] = 2048L
        bytesByOsd["osd.5"] = 2048L
        reconnect()
        assert primarySnapshotSize() == 1364L : "Sum the overlapping groups once and keep the independent group's rounding"
        bytesByOsd.clear()

        copiesByPool[pools[0].poolName] = 2
        snapshotBytes = 12 * mib
        reconnect()
        assert primarySnapshotSize() == 30 * mib : "Shared OSDs use the lower utilization, private OSDs use their own pool's utilization"
        copiesByPool.clear()
        osdsByPool.clear()
        snapshotBytes = 0L
        externalOsds = ["osd.2", "osd.99"]
        reconnect()
        long sharedBaseline = capacity().availableCapacity
        snapshotBytes = 48 * mib
        reconnect()
        assert primarySnapshotSize() == 48 * mib : "Include osd.2 once and exclude the external-only osd.99"
        assert capacity().availableCapacity == sharedBaseline - 48 * mib
    }

    void testBatchRecalculateWithEmptyPrimaryStorage(PrimaryStorageInventory ceph, long expectedCephAvailable) {
        def empty = addSharedMountPointPrimaryStorage {
            name = "empty-smp"
            url = "/empty-smp"
            zoneUuid = ceph.zoneUuid
        }
        long gib = SizeUnit.GIGABYTE.toByte(1)
        SQL.New(PrimaryStorageCapacityVO.class).eq(PrimaryStorageCapacityVO_.uuid, empty.uuid)
                .set(PrimaryStorageCapacityVO_.totalCapacity, 10 * gib)
                .set(PrimaryStorageCapacityVO_.systemUsedCapacity, gib)
                .set(PrimaryStorageCapacityVO_.availableCapacity, 0L).update()

        new PrimaryStorageCapacityRecalculator(psUuids: [ceph.uuid, empty.uuid]).recalculate()
        assert Q.New(PrimaryStorageCapacityVO.class).eq(PrimaryStorageCapacityVO_.uuid, empty.uuid)
                .select(PrimaryStorageCapacityVO_.availableCapacity).findValue() == 9 * gib :
                "Batch recalculation must restore the empty storage's balance and reserve its system usage"
        assert (getPrimaryStorageCapacity { primaryStorageUuids = [ceph.uuid] }).availableCapacity == expectedCephAvailable
    }

    void testSkipCalculateCapacityWhichInstallPathIsNull() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory

        VolumeSnapshotInventory rootSnapshot = createVolumeSnapshot {
            name = "root-volume-snapshot"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

        String volumeInstallPath = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.rootVolumeUuid).select(VolumeVO_.installPath)
                .findValue()
        String volumeSnapshotInstallPath = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, rootSnapshot.uuid)
                .select(VolumeSnapshotVO_.primaryStorageInstallPath)
                .findValue()

        // mock install path is null
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.rootVolumeUuid).set(VolumeVO_.installPath, null).update()
        SQL.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, rootSnapshot.uuid)
                .set(VolumeSnapshotVO_.primaryStorageInstallPath, null).update()

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.rootVolumeUuid).set(VolumeVO_.installPath, volumeInstallPath).update()
        SQL.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, rootSnapshot.uuid)
                .set(VolumeSnapshotVO_.primaryStorageInstallPath, volumeSnapshotInstallPath).update()
    }

}
