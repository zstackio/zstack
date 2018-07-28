package org.zstack.test.integration.storage.primary.ceph

import org.zstack.core.gc.GCStatus
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.ceph.CephGlobalConfig
import org.zstack.storage.ceph.primary.CephDeleteVolumeGC
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by kayo on 2018/7/25.
 */
class CephGCCase extends SubCase {
    EnvSpec env

    PrimaryStorageInventory ceph
    DiskOfferingInventory diskOffering

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

    void testVolumeGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        env.afterSimulator(CephPrimaryStorageBase.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorInventory inv = null

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(CephPrimaryStorageBase.DELETE_PATH) { rsp ->
            called = true
            return rsp
        }

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString()]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testVolumeGCCancelledAfterPrimaryStorageDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ceph.uuid
        }

        env.afterSimulator(CephPrimaryStorageBase.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorInventory volumeGC = null
        retryInSecs {
            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${CephDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert volumeGC.status == GCStatus.Idle.toString()
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = (env.specByName("test-cluster") as ClusterSpec).inventory.uuid
            primaryStorageUuid = ceph.uuid
        }

        deletePrimaryStorage {
            uuid = ceph.uuid
        }

        triggerGCJob {
            uuid = volumeGC.uuid
        }

        retryInSecs {
            // trigger GC cause it's cancelled
            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${CephDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert volumeGC.status == GCStatus.Done.toString()
        }
    }

    @Override
    void test() {
        env.create {
            ceph = (env.specByName("ceph-pri") as PrimaryStorageSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory

            // set a very long time so the GC won't run, we use API to trigger it
            CephGlobalConfig.GC_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

            testVolumeGCSuccess()
            testVolumeGCCancelledAfterPrimaryStorageDeleted()
        }
    }
}
