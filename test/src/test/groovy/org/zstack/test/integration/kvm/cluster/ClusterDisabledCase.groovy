package org.zstack.test.integration.kvm.cluster

import org.zstack.header.cluster.ClusterStateEvent
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by AlanJager on 2017/4/11.
 */
class ClusterDisabledCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmTwoHostsEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmOnDisabledCluster()
            testCreateVmWithoutSpecificClusterWhenClusterDisabled()
        }
    }

    void testCreateVmOnDisabledCluster() {
        InstanceOfferingInventory instanceOfferingInventory = env.inventoryByName("instanceOffering")
        ImageInventory imageInventory = env.inventoryByName("image1")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")
        ClusterInventory clusterInventory = env.inventoryByName("cluster1")
        changeClusterState {
            uuid = clusterInventory.uuid
            stateEvent = ClusterStateEvent.disable.toString()
        }

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "test"
        action.clusterUuid = clusterInventory.uuid
        action.imageUuid = imageInventory.uuid
        action.l3NetworkUuids = [l3NetworkInventory.uuid]
        action.instanceOfferingUuid = instanceOfferingInventory.uuid
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result ret = action.call()

        assert ret.error != null

        changeClusterState {
            uuid = clusterInventory.uuid
            stateEvent = ClusterStateEvent.enable.toString()
        }
    }

    void testCreateVmWithoutSpecificClusterWhenClusterDisabled() {
        InstanceOfferingInventory instanceOfferingInventory = env.inventoryByName("instanceOffering")
        ImageInventory imageInventory = env.inventoryByName("image1")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")
        ClusterInventory clusterInventory = env.inventoryByName("cluster1")
        ClusterInventory clusterInventory2 = env.inventoryByName("cluster2")
        changeClusterState {
            uuid = clusterInventory.uuid
            stateEvent = ClusterStateEvent.disable.toString()
        }
        changeClusterState {
            uuid = clusterInventory2.uuid
            stateEvent = ClusterStateEvent.disable.toString()
        }

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "test"
        action.imageUuid = imageInventory.uuid
        action.l3NetworkUuids = [l3NetworkInventory.uuid]
        action.instanceOfferingUuid = instanceOfferingInventory.uuid
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result ret = action.call()

        assert ret.error != null
    }
}
