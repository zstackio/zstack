package org.zstack.test.integration.core.trash

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.jsonlabel.JsonLabel
import org.zstack.core.jsonlabel.JsonLabelVO
import org.zstack.core.trash.StorageRecycleImpl
import org.zstack.header.core.trash.InstallPathRecycleVO
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2019/9/20.*/
class UpgradeOldVersionTrashCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    StorageRecycleImpl trashMrg
    DatabaseFacade dbf

    String trash1 = '''
{"resourceUuid":"volUuid","resourceType":"VolumeVO","storageUuid":"psUuid","storageType":"PrimaryStorageVO","installPath":"installPath1.qcow2","isFolder":false,"hypervisorType":"KVM","size":85899345920,"trashType":"RevertVolume"}
'''
    String trash2 = '''
{"resourceUuid":"volUuid","resourceType":"VolumeVO","storageUuid":"psUuid","storageType":"PrimaryStorageVO","installPath":"installPath2.qcow2","isFolder":false,"hypervisorType":"KVM","size":214748364800,"trashType":"ReimageVolume"}
'''
    String uuid1 = Platform.uuid
    String uuid2 = Platform.uuid

    long count = 0

    @Override
    void clean() {
        SQL.New(InstallPathRecycleVO.class).hardDelete()
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = TrashEnv.psbs()
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            prepare()
            testDatabase()
        }
    }

    void prepare() {
        ps = env.inventoryByName("ceph-ps") as PrimaryStorageInventory
        trashMrg = bean(StorageRecycleImpl.class)
        trash1 = trash1.replaceAll("psUuid", ps.uuid).replaceAll("volUuid", uuid1).trim()
        trash2 = trash2.replaceAll("psUuid", ps.uuid).replaceAll("volUuid", uuid2).trim()
        count = Q.New(JsonLabelVO.class).count()

        def lable = new JsonLabel()
        lable.create("RevertVolume-${Platform.uuid}", trash1, ps.uuid)
        lable = new JsonLabel()
        lable.create("ReimageVolume-${Platform.uuid}", trash2, ps.uuid)
    }

    void testDatabase() {
        sleep 200
        assert count == Q.New(JsonLabelVO.class).count()
        List<InstallPathRecycleVO> vos = Q.New(InstallPathRecycleVO.class).list()
        assert vos.size() == 2
        vos.each {
            if (it.trashType == "RevertVolume") {
                assert it.storageUuid == ps.uuid
                assert it.resourceUuid == uuid1
                assert it.resourceType == "VolumeVO"
                assert it.storageType == "PrimaryStorageVO"
                assert it.installPath == "installPath1.qcow2"
                assert it.size == 85899345920
            } else if (it.trashType == "ReimageVolume") {
                assert it.storageUuid == ps.uuid
                assert it.resourceUuid == uuid2
                assert it.resourceType == "VolumeVO"
                assert it.storageType == "PrimaryStorageVO"
                assert it.installPath == "installPath2.qcow2"
                assert it.size == 214748364800
            } else {
                assert false
            }
        }
    }
}
