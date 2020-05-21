package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.ceph.CephGlobalConfig
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class StartVmWhenRootVolumeWithWatchersCase extends SubCase{
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
            testStartVmWhenRootVolumeWithWatchers()
        }
    }

    void testStartVmWhenRootVolumeWithWatchers(){
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory
        stopVmInstance{
            uuid = vm.uuid
        }

        env.simulator(CephPrimaryStorageBase.GET_IMAGE_WATCHERS_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.GetVolumeWatchersRsp()
            rsp.watchers = [
                    "watcher=10.0.0.12Connection closed by foreign host.ie=86158848"
            ]
            return rsp
        }

        expectError {
           startVmInstance {
               uuid = vm.uuid
           }
        }

        CephGlobalConfig.PREVENT_VM_SPLIT_BRAIN.updateValue("false")
        startVmInstance {
            uuid = vm.uuid
        }
    }
    
    @Override
    void clean() {
        env.delete()
    }
}
