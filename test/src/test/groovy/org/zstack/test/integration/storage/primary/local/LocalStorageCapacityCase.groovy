package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.LocalStorageSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/4/13.
 */
class LocalStorageCapacityCase extends SubCase {
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
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCalculationOfLSCapacityWhenDetachAndAttachLSPStoCluster()
        }
    }

    void testCalculationOfLSCapacityWhenDetachAndAttachLSPStoCluster() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("iso")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        createVmInstance {
            name = "test"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            rootDiskOfferingUuid = diskOffering.uuid
        }
        GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availableCapacity > capacityResult.availableCapacity

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
        env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            LocalStorageSpec lspec = spec.specByUuid(ps.uuid)

            def rsp = new LocalStorageKvmBackend.AgentResponse()
            rsp.totalCapacity = lspec.totalCapacity
            rsp.availableCapacity = lspec.availableCapacity
            return rsp
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        GetPrimaryStorageCapacityResult capacityResult2 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert capacityResult.availableCapacity == capacityResult2.availableCapacity

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        GetPrimaryStorageCapacityResult capacityResult3 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert capacityResult2.availableCapacity == capacityResult3.availableCapacity
        assert capacityResult.availableCapacity == capacityResult2.availableCapacity
    }
}
