package org.zstack.test.integration.storage.primary.nfs

import org.zstack.core.gc.GCStatus
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.nfs.NfsDeleteVolumeGC
import org.zstack.storage.primary.nfs.NfsPrimaryStorageGlobalConfig
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/5.
 */
class NfsGCCase extends SubCase {
    EnvSpec env

    PrimaryStorageInventory nfs
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
        env = Env.nfsOneVmEnv()
    }

    void testVolumeGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }

        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
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
        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) { rsp ->
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


    void testVolumeSnapshotGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "sp"
        }

        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }

        GarbageCollectorInventory inv = null
        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) { rsp ->
            called = true
            return rsp
        }

        triggerGCJob {
            uuid = inv.uuid
        }

        retryInSecs {
            inv = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString()]
            }[0]

            assert called
            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testVolumeSnapshotAndVolumeGCCancelledAfterPrimaryStorageDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }

        VolumeSnapshotInventory sp = createVolumeSnapshot {
            volumeUuid = vol.uuid
            name = "sp"
        }

        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteVolumeSnapshot {
            uuid = sp.uuid
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        GarbageCollectorInventory spGC = null
        GarbageCollectorInventory volumeGC = null
        retryInSecs {
            spGC = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString()]
            }[0]

            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${NfsDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert spGC.status == GCStatus.Idle.toString()
            assert volumeGC.status == GCStatus.Idle.toString()
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = (env.specByName("cluster") as ClusterSpec).inventory.uuid
            primaryStorageUuid = nfs.uuid
        }

        deletePrimaryStorage {
            uuid = nfs.uuid
        }

        triggerGCJob {
            uuid = spGC.uuid
        }

        retryInSecs {
            // trigger GC cause it's cancelled
            spGC = queryGCJob {
                conditions = ["context~=%${sp.getUuid()}%".toString()]
            }[0]

            assert spGC.status == GCStatus.Done.toString()
        }

        triggerGCJob {
            uuid = volumeGC.uuid
        }

        retryInSecs {
            // trigger GC cause it's cancelled
            volumeGC = queryGCJob {
                conditions = ["context~=%${vol.getUuid()}%".toString(), "runnerClass=${NfsDeleteVolumeGC.class.name}".toString()]
            }[0]

            assert volumeGC.status == GCStatus.Done.toString()
        }
    }

    @Override
    void test() {
        env.create {
            nfs = (env.specByName("nfs") as PrimaryStorageSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory

            // set a very long time so the GC won't run, we use API to trigger it
            NfsPrimaryStorageGlobalConfig.GC_INTERVAL.updateValue(TimeUnit.DAYS.toSeconds(1))
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

            testVolumeGCSuccess()
            testVolumeSnapshotGCSuccess()
            testVolumeSnapshotAndVolumeGCCancelledAfterPrimaryStorageDeleted()
        }
    }
}
