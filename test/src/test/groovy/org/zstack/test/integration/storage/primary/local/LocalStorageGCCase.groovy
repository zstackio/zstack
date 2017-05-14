package org.zstack.test.integration.storage.primary.local

import org.zstack.core.gc.GCStatus
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.ClusterSpec
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/3/6.
 */
class LocalStorageGCCase extends SubCase {
    EnvSpec env

    DiskOfferingInventory diskOffering
    PrimaryStorageInventory local
    HostInventory host

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
        env = Env.localStorageOneVmEnv()
    }

    void testGCSuccess() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }

        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        boolean called = false
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { rsp ->
            called = true
            return rsp
        }

        // reconnect host to trigger the GC
        reconnectHost {
            uuid = host.uuid
        }

        retryInSecs {
            assert called
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testGCCancelledAfterHostDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }

        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        deleteHost {
            uuid = host.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }
    }

    void testGCCancelledAfterPrimaryStorageDeleted() {
        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }

        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) {
            throw new HttpError(403, "on purpose")
        }

        deleteDataVolume {
            uuid = vol.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = (env.specByName("cluster") as ClusterSpec).inventory.uuid
            primaryStorageUuid = local.uuid
        }

        deletePrimaryStorage {
            uuid = local.uuid
        }

        retryInSecs {
            GarbageCollectorInventory inv = queryGCJob {
                conditions = ["context~=%${vol.uuid}%".toString()]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }
    }

    @Override
    void test() {
        env.create {
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

            local = (env.specByName("local") as PrimaryStorageSpec).inventory
            diskOffering = (env.specByName("diskOffering") as DiskOfferingSpec).inventory
            host = (env.specByName("kvm") as HostSpec).inventory

            testGCSuccess()
            testGCCancelledAfterHostDeleted()

            host = (env.recreate("kvm") as HostSpec).inventory

            testGCCancelledAfterPrimaryStorageDeleted()
        }
    }
}
