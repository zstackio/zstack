package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.header.host.HostVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.LocalStorageSpec
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
            testCalculationOfLSCapacityWhenDetachAndAttachLSPStoCluster()
            testLocalStoragePrimaryStorageCapacityDecreaseAfterDeleteHost()
            testCreateVmChangePSCapacity()
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
        LocalStorageKvmBackend.InitCmd cmd = null
        env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            cmd = json(e.body, LocalStorageKvmBackend.InitCmd.class)
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

        GetPrimaryStorageCapacityResult capacityResult2
        retryInSecs {
            capacityResult2 = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }

            return {
                assert capacityResult.availableCapacity == capacityResult2.availableCapacity
            }
        }

        retryInSecs {
            return {
                assert cmd != null
            }
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        retryInSecs {
            GetPrimaryStorageCapacityResult capacityResult3 = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }

            return {
                assert capacityResult2.availableCapacity == capacityResult3.availableCapacity
                assert capacityResult.availableCapacity == capacityResult2.availableCapacity
            }
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

    void testCreateVmChangePSCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("image1")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        LocalStorageKvmBackend.CreateVolumeFromCacheCmd cmd = null
        env.afterSimulator(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, LocalStorageKvmBackend.CreateVolumeFromCacheCmd.class)
            rsp = new LocalStorageKvmBackend.CreateVolumeFromCacheRsp()
            rsp.setTotalCapacity(10000)
            rsp.setAvailableCapacity(10000)
            return rsp
        }

        createVmInstance {
            name = "test"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        assert retryInSecs {
            return cmd != null
        }

        GetPrimaryStorageCapacityResult result = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert result.totalPhysicalCapacity < beforeCapacityResult.totalPhysicalCapacity
        assert result.availablePhysicalCapacity < beforeCapacityResult.availablePhysicalCapacity
    }
}

