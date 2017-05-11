package org.zstack.test.integration.storage.primary.ceph.secret

import org.springframework.http.HttpEntity
import org.zstack.sdk.VmInstanceInventory
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

        CephPrimaryStorageBase.CreateKvmSecretCmd cmd
        env.afterSimulator(CephPrimaryStorageBase.KVM_CREATE_SECRET_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, CephPrimaryStorageBase.CreateKvmSecretCmd.class)
            return rsp
        }

        startVmInstance {
            uuid = vm.uuid
        }
        assert null != cmd
        assert dbFindByUuid(psuuid, CephPrimaryStorageVO.class).userKey == cmd.userKey
        assert CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(psuuid, CephSystemTags.KVM_SECRET_UUID_TOKEN) == cmd.uuid
    }

    void createSecretWhenCreateVm(){
        VmInstanceInventory vm = env.inventoryByName("test-vm")
        String psuuid = vm.allVolumes[0].primaryStorageUuid

        CephPrimaryStorageBase.CreateKvmSecretCmd cmd
        env.afterSimulator(CephPrimaryStorageBase.KVM_CREATE_SECRET_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, CephPrimaryStorageBase.CreateKvmSecretCmd.class)
            return rsp
        }

        createVmInstance {
            name = "newVm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
        }
        assert null != cmd
        assert dbFindByUuid(psuuid, CephPrimaryStorageVO.class).userKey == cmd.userKey
        assert CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(psuuid, CephSystemTags.KVM_SECRET_UUID_TOKEN) == cmd.uuid
    }
    
    @Override
    void clean() {
        env.delete()
    }
}