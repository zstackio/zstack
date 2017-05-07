package org.zstack.test.integration.storage.primary.nfs

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQLBatch
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by Administrator on 2017-05-08.
 */
class NfsTwoHostCase extends SubCase{
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
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testNfsRefVO()
        }
    }

    void testNfsRefVO(){
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        PrimaryStorageInventory ps = env.inventoryByName("nfs") as PrimaryStorageInventory
        def dbf = bean(DatabaseFacade)



        new SQLBatch(){
            @Override
            protected void scripts() {
                //PrimaryStorageHostRefVO ref1 = new PrimaryStorageHostRefVO()
                PrimaryStorageHostRefVO ref2 = new PrimaryStorageHostRefVO()

                //ref1.setPrimaryStorageUuid(ps.uuid)
                //ref1.setHostUuid(host.uuid)
                //ref1.setStatus(PrimaryStorageHostStatus.Connected)

                ref2.setPrimaryStorageUuid(ps.uuid)
                ref2.setHostUuid(host.uuid)
                ref2.setStatus(PrimaryStorageHostStatus.Disconnected)

                merge(ref2)
            }
        }.execute()


    }
}
