package org.zstack.test.integration.storage.snapshot

import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.kvm.KVMConstant
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator
import org.zstack.core.db.DatabaseFacade

/**
 * Created by ads6 on 2018/1/2.
 */
class RevertVolumeFromSnapshotCase extends SubCase{
    def DOC = """
STEP:
1. Create 3 snapshot on root 
2. revert first snapshot on volume
3. create 2 more snapshot 
"""

    EnvSpec env
    VmInstanceInventory vm

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
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testRevertVolumeFromSnapshot()
        }
    }

    void testRevertVolumeFromSnapshot(){
        VolumeSnapshotInventory rootSnapshotInv = createSnapshot(vm.getRootVolumeUuid())
        createSnapshot(vm.getRootVolumeUuid())
        createSnapshot(vm.getRootVolumeUuid())

        stopVmInstance {
            uuid = vm.uuid
        }

        revertVolumeFromSnapshot {
            uuid = rootSnapshotInv.uuid
        }

        assert Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.latest).eq(VolumeSnapshotVO_.uuid, rootSnapshotInv.uuid).findValue()
        assert Q.New(VolumeSnapshotVO.class).count() == 3

        VolumeSnapshotInventory snapshot1 =  createSnapshot(vm.getRootVolumeUuid())
        VolumeSnapshotInventory snapshot2 = createSnapshot(vm.getRootVolumeUuid())

        VolumeSnapshotVO s1vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot1.uuid).find()
        VolumeSnapshotVO s2vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot2.uuid).find()
        assert s1vo.getDistance() == 2
        assert s2vo.getDistance() == 3
        assert s2vo.getParentUuid() == s1vo.getUuid()
        assert s1vo.getParentUuid() == rootSnapshotInv.uuid
        assert !s1vo.isLatest()
        assert s2vo.isLatest()
        assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.latest, true).count() == 1
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 1
    }

    private VolumeSnapshotInventory createSnapshot(String uuid){
        VolumeSnapshotInventory inv = createVolumeSnapshot {
            volumeUuid = uuid
            name = "test-snapshot"
        } as VolumeSnapshotInventory
        return inv
    }



}
