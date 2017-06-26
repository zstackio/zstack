package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase


/**
 * Created by MaJin on 2017-05-23.
 */
class VmOperationMultyTypeStorageCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageNfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmChooseNfs()
            testDisableNfsPrimaryStorageThenCreateVmInstance()
            testDisableNfsPrimaryStorageThenAttachDataVolumeToVm()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCreateVmChooseNfs(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        InstanceOfferingInventory ins = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.name = "vm"
        a.instanceOfferingUuid = ins.uuid
        a.imageUuid = image.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.primaryStorageUuidForRootVolume = nfs.uuid
        a.sessionId = currentEnvSpec.session.uuid

        assert a.call().error != null
    }

    void testDisableNfsPrimaryStorageThenCreateVmInstance(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        InstanceOfferingInventory ins = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.name = "vm1"
        a.instanceOfferingUuid = ins.uuid
        a.imageUuid = image.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.dataDiskOfferingUuids = [diskOfferingInventory.uuid]
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null
    }

    void testDisableNfsPrimaryStorageThenAttachDataVolumeToVm(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        InstanceOfferingInventory ins = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm2"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = ins.uuid
        }

        VolumeInventory volume = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOfferingInventory.uuid
        }

        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction(
                vmInstanceUuid: vm.uuid,
                volumeUuid: volume.uuid,
                sessionId: currentEnvSpec.session.uuid
        )
        assert action.call().error != null
    }
}
