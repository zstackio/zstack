package org.zstack.test.integration.storage.primary.ceph

import org.zstack.sdk.GetVolumeSnapshotSizeResult
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2019/5/14.
 */
class GetVolumeSnapshotSizeCase extends SubCase{
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
            testGetVolumeSnapshotSize()
        }
    }

    void testGetVolumeSnapshotSize(){
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory

        VolumeSnapshotInventory snapshotInventory = createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "test_snap"
        }

        long size = 1
        long actualSize = 10

        env.simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPSHOT_SIZE_PATH) {
            def rsp = new CephPrimaryStorageBase.GetVolumeSnapshotSizeRsp()
            rsp.size = size
            rsp.actualSize = actualSize
            return rsp
        }

        GetVolumeSnapshotSizeResult result = getVolumeSnapshotSize {
            uuid = snapshotInventory.uuid
        }
        assert result.actualSize == actualSize
        assert result.size == size

        env.simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPSHOT_SIZE_PATH) {
            def rsp = new CephPrimaryStorageBase.GetVolumeSnapshotSizeRsp()
            rsp.size = size
            return rsp
        }

        result = getVolumeSnapshotSize {
            uuid = snapshotInventory.uuid
        }
        assert result.actualSize == null
        assert result.size == size
    }
    
    @Override
    void clean() {
        env.delete()
    }
}
