package org.zstack.test.integration.storage.primary.local

import org.zstack.header.host.HostVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

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
            testLocalStoragePrimaryStorageCapacityDecreaseAfterDeleteHost()
        }
    }

    void testLocalStoragePrimaryStorageCapacityDecreaseAfterDeleteHost() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster")

        GetPrimaryStorageCapacityResult result = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        HostInventory host = addKVMHost {
            name = "test"
            managementIp = "127.0.0.2"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        }
        assert dbFindByUuid(host.uuid, HostVO.class) != null

        GetPrimaryStorageCapacityResult result2 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert result2.totalCapacity > result.totalCapacity

        deleteHost {
            uuid = host.uuid
        }

        GetPrimaryStorageCapacityResult result3 = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert result3.totalCapacity == result.totalCapacity
    }
}
