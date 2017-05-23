package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
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
}
