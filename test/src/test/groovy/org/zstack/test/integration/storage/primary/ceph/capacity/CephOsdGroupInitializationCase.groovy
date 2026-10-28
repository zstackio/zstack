package org.zstack.test.integration.storage.primary.ceph.capacity

import org.zstack.core.db.Q
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.storage.ceph.CephCapacity
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.primary.CephOsdGroupVO
import org.zstack.storage.ceph.primary.CephOsdGroupVO_
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO_
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO
import org.zstack.storage.ceph.primary.capacity.CephOsdGroupCapacityHelper
import org.zstack.storage.ceph.primary.capacity.CephPrimaryCapacityUpdater
import org.zstack.storage.ceph.primary.capacity.OpenSourceCephPrimaryCapacityUpdater
import org.zstack.storage.ceph.primary.capacity.XSKYCephPrimaryCapacityBaseUpdater
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class CephOsdGroupInitializationCase extends SubCase {
    EnvSpec env
    String psUuid
    String fsid
    long primaryStorageTotal
    List<String> poolNames
    String unchangedPoolName = "unchanged-pool"

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
            psUuid = env.inventoryByName("ceph-pri").uuid
            primaryStorageTotal = env.specByUuid(psUuid).totalCapacity
            fsid = dbFindByUuid(psUuid, CephPrimaryStorageVO.class).fsid
            env.afterSimulator(CephPrimaryStorageBase.ADD_POOL_PATH) { CephPrimaryStorageBase.AddPoolRsp rsp ->
                rsp.totalCapacity = primaryStorageTotal
                rsp.availableCapacity = primaryStorageTotal
                return rsp
            }
            addCephPrimaryStoragePool {
                primaryStorageUuid = psUuid
                poolName = unchangedPoolName
                type = "Data"
                isCreate = true
            }
            poolNames = Q.New(CephPrimaryStoragePoolVO.class)
                    .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, psUuid)
                    .select(CephPrimaryStoragePoolVO_.poolName).listValues().unique()
            PrimaryStorageOverProvisioningManager ratioMgr = bean(PrimaryStorageOverProvisioningManager.class)
            double oldRatio = ratioMgr.getRatio(psUuid)
            ratioMgr.setRatio(psUuid, 2)
            try {
                [new OpenSourceCephPrimaryCapacityUpdater(), new XSKYCephPrimaryCapacityBaseUpdater()].each { updater ->
                    testNewGroupAndRepeatedReport(updater)
                    testPoolGroupChanges(updater)
                }
            } finally {
                ratioMgr.setRatio(psUuid, oldRatio)
            }
        }
    }

    private CephCapacity report(String osds, long poolTotal) {
        return new CephCapacity(fsid: fsid, totalCapacity: primaryStorageTotal, availableCapacity: primaryStorageTotal,
                poolCapacities: poolNames.collect { pool ->
                    new CephPoolCapacity(name: pool, relatedOsds: osds, totalCapacity: poolTotal,
                            availableCapacity: poolTotal, usedCapacity: 0, replicatedSize: 3,
                            diskUtilization: 1.0F, securityPolicy: "replicated")
                })
    }

    private CephOsdGroupVO group(String osds) {
        return Q.New(CephOsdGroupVO.class).eq(CephOsdGroupVO_.primaryStorageUuid, psUuid)
                .eq(CephOsdGroupVO_.osds, osds).find()
    }

    void testNewGroupAndRepeatedReport(CephPrimaryCapacityUpdater updater) {
        long volumeSize = Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, psUuid).list()
                .sum { VolumeVO volume -> volume.size } as long
        long cacheSize = (Q.New(ImageCacheVO.class).eq(ImageCacheVO_.primaryStorageUuid, psUuid).list()
                .sum { ImageCacheVO cache -> cache.size } ?: 0) as long
        long snapshotSize = (Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.primaryStorageUuid, psUuid).list()
                .sum { VolumeSnapshotVO snapshot -> snapshot.size } ?: 0) as long
        long used = volumeSize / 2 + cacheSize + snapshotSize
        assert used > 0

        String osds = "new-${updater.cephManufacturer}"
        long total = SizeUnit.GIGABYTE.toByte(200)
        CephCapacity capacity = report(osds, total)
        updater.update(capacity)
        CephOsdGroupVO initialized = group(osds)
        assert initialized.availableCapacity == total - used

        VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, psUuid).find()
        long reserved = SizeUnit.GIGABYTE.toByte(3)
        CephOsdGroupCapacityHelper helper = new CephOsdGroupCapacityHelper(psUuid)
        helper.reserveAvailableCapacity(volume.installPath, reserved)
        updater.update(capacity)
        assert group(osds).uuid == initialized.uuid
        assert group(osds).availableCapacity == total - used - reserved
        helper.releaseAvailableCapacity(volume.installPath, reserved)

        updater.update(report(osds + "-full", used))
        assert group(osds + "-full").availableCapacity == 0
        updater.update(report(osds + "-overcommitted", used - 1))
        assert group(osds + "-overcommitted").availableCapacity == -1
    }

    void testPoolGroupChanges(CephPrimaryCapacityUpdater updater) {
        VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, psUuid).find()
        String volumePool = volume.installPath.split("/")[2]
        List<String> otherPools = poolNames.findAll { it != volumePool && it != unchangedPoolName }
        assert otherPools.size() == 2 : "Expected two default pools besides the root pool, actual=${otherPools}"
        long movedUsed = volume.size / 2
        long total = primaryStorageTotal
        String source = "source-${updater.cephManufacturer}"
        String target = "target-${updater.cephManufacturer}"
        String unchanged = "unchanged-${updater.cephManufacturer}"
        String split = "split-${updater.cephManufacturer}"
        CephCapacity capacity = report(source, total)
        capacity.poolCapacities.find { it.name == otherPools[0] }.relatedOsds = target
        capacity.poolCapacities.find { it.name == unchangedPoolName }.relatedOsds = unchanged
        updater.update(capacity)

        String sourceUuid = group(source).uuid
        String targetUuid = group(target).uuid
        long sourceAvailable = group(source).availableCapacity
        long targetAvailable = group(target).availableCapacity
        long unchangedAvailable = group(unchanged).availableCapacity
        long reserved = SizeUnit.GIGABYTE.toByte(3)
        String unchangedPath = "ceph://${unchangedPoolName}/reserved-volume"
        CephOsdGroupCapacityHelper helper = new CephOsdGroupCapacityHelper(psUuid)
        helper.reserveAvailableCapacity(unchangedPath, reserved)
        assertGroupCapacity(unchanged, unchangedAvailable - reserved)
        capacity.poolCapacities.find { it.name == volumePool }.relatedOsds = target
        updater.update(capacity)
        assert group(source).uuid == sourceUuid : "Migration must reuse source group ${sourceUuid}, actual=${group(source).uuid}"
        assert group(target).uuid == targetUuid : "Migration must reuse target group ${targetUuid}, actual=${group(target).uuid}"
        assertGroupCapacity(source, sourceAvailable + movedUsed)
        assertGroupCapacity(target, targetAvailable - movedUsed)
        assertGroupCapacity(unchanged, unchangedAvailable)

        helper.reserveAvailableCapacity(volume.installPath, reserved)
        updater.update(capacity)
        assertGroupCapacity(target, targetAvailable - movedUsed - reserved)
        helper.releaseAvailableCapacity(volume.installPath, reserved)

        capacity.poolCapacities.find { it.name == volumePool }.relatedOsds = split
        updater.update(capacity)
        assertGroupCapacity(target, targetAvailable)
        assertGroupCapacity(split, total - movedUsed)
        assertGroupCapacity(unchanged, unchangedAvailable)

        capacity.poolCapacities.find { it.name == volumePool }.relatedOsds = target
        updater.update(capacity)
        assert group(split) == null : "Merged group ${split} should be removed, actual=${group(split)?.uuid}"
        assertGroupCapacity(target, targetAvailable - movedUsed)
        assertGroupCapacity(source, sourceAvailable + movedUsed)
        assertGroupCapacity(unchanged, unchangedAvailable)
    }

    private void assertGroupCapacity(String osds, long expected) {
        long actual = group(osds).availableCapacity
        assert actual == expected : "Group ${osds} has incorrect virtual availableCapacity: expected=${expected}, actual=${actual}"
    }

    @Override
    void clean() {
        env.delete()
    }
}
