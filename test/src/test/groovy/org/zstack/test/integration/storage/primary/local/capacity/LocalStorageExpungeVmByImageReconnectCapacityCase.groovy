package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.LocalStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.testlib.VmSpec
import org.zstack.core.db.DatabaseFacade
import org.zstack.kvm.KVMAgentCommands
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.test.integration.storage.Env
import org.zstack.storage.primary.local.LocalStorageKvmBackend


/**
 * Created by SyZhao on 2017/4/21.
 */
class LocalStorageExpungeVmByImageReconnectCapacityCase extends SubCase {
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
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void test() {
        env.create {
            testExpungeVmByImageReconnectCheckCapacity()
        }
    }

    void testExpungeVmByImageReconnectCheckCapacity() {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Delay.toString())
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("image1")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        VmSpec vmSpec = env.specByName("test-vm")
        VmInstanceInventory vm = vmSpec.inventory
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        assert vmSpec.inventory.rootVolumeUuid


        LocalStorageHostRefVO beforeRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        int spend = 1000000000
        boolean checked = false
        env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            LocalStorageKvmBackend.InitCmd cmd = JSONObjectUtil.toObject(e.body,LocalStorageKvmBackend.InitCmd.class)

            LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                    .eq(LocalStorageHostRefVO_.hostUuid, cmd.hostUuid).find()

            def rsp = new LocalStorageKvmBackend.AgentResponse()
            rsp.totalCapacity = refVO.totalPhysicalCapacity
            if(cmd.hostUuid == vm.hostUuid){
                rsp.availableCapacity = refVO.totalPhysicalCapacity - spend
            }else{
                rsp.availableCapacity = refVO.availablePhysicalCapacity
            }
            checked = true
            return rsp
        }
        //reconnectHost {
        //    uuid = vm.hostUuid
        //}
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        GetPrimaryStorageCapacityResult afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        LocalStorageHostRefVO afterRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert checked
        assert beforeCapacityResult.availablePhysicalCapacity ==  afterCapacityResult.availablePhysicalCapacity + spend
        //assert beforeCapacityResult.availableCapacity > afterCapacityResult.availableCapacity
        assert beforeRefVO.availablePhysicalCapacity == afterRefVO.availablePhysicalCapacity + spend


        destroyVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Destroyed

        def delete_bits_path_is_invoked = false
        env.simulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { HttpEntity<String> e, EnvSpec spec ->
            delete_bits_path_is_invoked = true
            LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                    .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
            def rsp = new LocalStorageKvmBackend.AgentResponse()
            rsp.totalCapacity = refVO.totalPhysicalCapacity
            rsp.availableCapacity = refVO.availablePhysicalCapacity + spend
            return rsp
        }
        expungeVmInstance {
            uuid = vmSpec.inventory.uuid
        }
        assert delete_bits_path_is_invoked

        //GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
        //    primaryStorageUuids = [ps.uuid]
        //}
        //assert beforeCapacityResult.availableCapacity < capacityResult.availableCapacity
        //assert beforeCapacityResult.availablePhysicalCapacity < capacityResult.availablePhysicalCapacity

        env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            LocalStorageKvmBackend.InitCmd cmd = JSONObjectUtil.toObject(e.body,LocalStorageKvmBackend.InitCmd.class)

            LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                    .eq(LocalStorageHostRefVO_.hostUuid, cmd.hostUuid).find()

            def rsp = new LocalStorageKvmBackend.AgentResponse()
            rsp.totalCapacity = refVO.totalPhysicalCapacity
            rsp.availableCapacity = refVO.availablePhysicalCapacity
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity
        assert beforeCapacityResult.availableCapacity == afterCapacityResult.availableCapacity
    }
}
