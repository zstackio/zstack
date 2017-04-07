package org.zstack.test.integration.kvm.vm

import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.storage.CephEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

/**
 * Created by AlanJager on 2017/4/7.
 */
class VmStateSyncCase extends SubCase {
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
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testVmStateSyncOnCephPrimaryStorage()
        }
    }

    void testVmStateSyncOnCephPrimaryStorage() {
        InstanceOfferingInventory instanceOfferingInventory = env.inventoryByName("instanceOffering")
        ImageInventory imageInventory = env.inventoryByName("image")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")
        ClusterInventory clusterInventory = env.inventoryByName("test-cluster")

        for(int i = 0; i < 20; i++) {
            createVmInstance {
                name = "test " + i
                instanceOfferingUuid = instanceOfferingInventory.uuid
                imageUuid = imageInventory.uuid
                l3NetworkUuids = [l3NetworkInventory.uuid]
            }
        }

        env.simulator(KVMConstant.KVM_PING_PATH) {
            throw new HttpError(403, "on purpose")
        }

        List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.clusterUuid, clusterInventory.uuid).listValues()
        for(VmInstanceVO vm : vms) {
            assert vm.state == VmInstanceState.Unknown
        }

        env.cleanSimulatorHandlers()

        vms = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.clusterUuid, clusterInventory.uuid).listValues()
        for(VmInstanceVO vm : vms) {
            assert vm.state == VmInstanceState.Running
        }
    }
}
