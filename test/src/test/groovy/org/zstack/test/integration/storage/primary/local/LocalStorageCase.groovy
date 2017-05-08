package org.zstack.test.integration.storage.primary.local

import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by AlanJager on 2017/4/18.
 */
class LocalStorageCase extends SubCase {
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
            testDetachLocalStoragePrimaryStorageReleaseResource()
        }
    }

    void testDetachLocalStoragePrimaryStorageReleaseResource() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster")
        L3NetworkInventory l3 = env.inventoryByName("l3")


        GetIpAddressCapacityResult result = getIpAddressCapacity {
            l3NetworkUuids = [l3.uuid]
        }
        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = ps.uuid
        }

        GetIpAddressCapacityResult capacityResult = getIpAddressCapacity {
            l3NetworkUuids = [l3.uuid]
        }
        assert capacityResult.availableCapacity > result.availableCapacity
    }
}
