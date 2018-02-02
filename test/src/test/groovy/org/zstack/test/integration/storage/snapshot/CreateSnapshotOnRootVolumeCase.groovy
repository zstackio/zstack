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
class CreateSnapshotOnRootVolumeCase extends SubCase{
    def DOC = """
Step:
1. Create 5 snapshot on vm root volume
2. check the root and middle and last snapshot separately
3. check snapshotTree
4. delete snapshot[3]
5. assert snapshot[3] and snapshot[4] deleted

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
            testCreateSnapshotOnRootVolume()
        }
    }

    void testCreateSnapshotOnRootVolume(){
        def snapshotNum = 5
        def createSnapshotPathInvokedCount = 0
        env.afterSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH){ rsp ->
            createSnapshotPathInvokedCount++
            return rsp
        }

        List<VolumeSnapshotInventory> snapshotInvList = new ArrayList<>()
        for (int i=0; i<snapshotNum; i++){
            VolumeSnapshotInventory snapshotInv = createVolumeSnapshot {
                volumeUuid = vm.getRootVolumeUuid()
                name = "test"
            } as VolumeSnapshotInventory
            snapshotInvList.add(snapshotInv)
        }

        assert createSnapshotPathInvokedCount == snapshotNum
        assert Q.New(VolumeSnapshotVO.class).count() == snapshotNum


        firstSnapshot(snapshotInvList[0])

        for (int i=1; i<snapshotNum-1; i++){
            deltaSnapshot(snapshotInvList[i], i+1)
        }

        lastSnapshot(snapshotInvList[snapshotNum-1], snapshotNum)

        assert Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, snapshotInvList[0].getTreeUuid()).isExists()
        assert Q.New(VolumeSnapshotTreeVO.class).count() == 1


        deleteVolumeSnapshot {
            uuid = snapshotInvList[3].uuid
        }

        retryInSecs{
            assert dbFindByUuid(snapshotInvList[3].uuid, VolumeSnapshotVO.class) == null
            assert dbFindByUuid(snapshotInvList[4].uuid, VolumeSnapshotVO.class) == null
            assert Q.New(VolumeSnapshotVO.class).count() == 3
            assert Q.New(VolumeSnapshotTreeVO.class).count() == 1
        }
    }

    void firstSnapshot(VolumeSnapshotInventory snapshotInv){
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotVO snapshotVO =Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInv.getUuid()).find()
        assert snapshotVO.isFullSnapshot() == false
        assert snapshotVO.isLatest() == false
        assert snapshotVO.getParentUuid() == null
        assert snapshotVO.getDistance() == SnapshotTestConstant.ROOT_SNAPSHOT_DISNTANCE
        assert snapshotVO.getPrimaryStorageUuid() == volumeVO.getPrimaryStorageUuid()
        assert snapshotVO.getPrimaryStorageInstallPath() != null

    }

    void lastSnapshot(VolumeSnapshotInventory snapshotInv, int distance) {
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotVO snapshotVO = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInv.getUuid()).find()
        assert snapshotVO.isFullSnapshot() == false
        assert snapshotVO.isLatest()
        assert snapshotVO.getParentUuid()  != null
        assert snapshotVO.getDistance() == distance
        assert snapshotVO.getPrimaryStorageUuid() == volumeVO.getPrimaryStorageUuid()
        assert snapshotVO.getPrimaryStorageInstallPath() != null
    }


    void deltaSnapshot(VolumeSnapshotInventory snapshotInv, int distance){
        VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid()).find()
        VolumeSnapshotVO snapshotVO =Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInv.getUuid()).find()
        assert snapshotVO.isFullSnapshot() == false
        assert snapshotVO.isLatest() == false
        assert snapshotVO.getParentUuid() != null
        assert snapshotVO.getDistance() == distance
        assert snapshotVO.getPrimaryStorageUuid() == volumeVO.getPrimaryStorageUuid()
        assert snapshotVO.getPrimaryStorageInstallPath() != null

    }

}
