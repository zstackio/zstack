package org.zstack.test.integration.storage.primary.ceph.secret

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/05/09.
 */
class CreateAndStartVmCase extends SubCase{

    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            createSecretWhenStartVm()
            createSecretWhenCreateVm()
        }
    }

    void createSecretWhenStartVm(){

        VmInstanceInventory vm = env.inventoryByName("test-vm")
        String psuuid = vm.allVolumes[0].primaryStorageUuid

        stopVmInstance {
            uuid = vm.uuid
        }

        KVMAgentCommands.StartVmCmd cmd
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        startVmInstance {
            uuid = vm.uuid
        }
        assert null != cmd
        assert dbFindByUuid(psuuid, CephPrimaryStorageVO.class).userKey == cmd.addons.get(CephConstants.CEPH_SCECRET_KEY)
        assert CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(psuuid, CephSystemTags.KVM_SECRET_UUID_TOKEN) == cmd.addons.get(CephConstants.CEPH_SECRECT_UUID)
    }

    void createSecretWhenCreateVm(){
        VmInstanceInventory vm = env.inventoryByName("test-vm")
        String psuuid = vm.allVolumes[0].primaryStorageUuid

        KVMAgentCommands.StartVmCmd cmd
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        createVmInstance {
            name = "newVm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
        }
        assert null != cmd
        assert dbFindByUuid(psuuid, CephPrimaryStorageVO.class).userKey == cmd.addons.get(CephConstants.CEPH_SCECRET_KEY)
        assert CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(psuuid, CephSystemTags.KVM_SECRET_UUID_TOKEN) == cmd.addons.get(CephConstants.CEPH_SECRECT_UUID)
    }
    
    @Override
    void clean() {
        env.delete()
    }
}