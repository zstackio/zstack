package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.header.storage.backup.BackupStorageStatus
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMHost
import org.zstack.sdk.AttachIsoToVmInstanceAction
import org.zstack.storage.backup.BackupStorageBase
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.VmSpec
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest

import org.zstack.utils.gson.JSONObjectUtil
import sun.awt.image.ImageCache;
/**
 * Created by xing5 on 2017/2/27.
 */
class AttachIsoCase2 extends SubCase {
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
            testAttachIsoToDisabledCephStorageWhenIsoDeleted()
        }
    }

    @Override
    void clean(){
        env.delete()
    }



    void testAttachIsoToDisabledCephStorageWhenIsoDeleted(){
        PrimaryStorageSpec primaryStorageSpec = env.specByName("ceph-pri") as PrimaryStorageSpec
        BackupStorageSpec backupStorageSpec = env.specByName("ceph-bk") as BackupStorageSpec
        VmSpec vmSpec = env.specByName("test-vm") as VmSpec
        ImageSpec imageSpec = env.specByName("test-iso") as ImageSpec
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        assert primaryStorageSpec.inventory.uuid
        assert vmSpec.inventory.uuid
        assert imageSpec.inventory.uuid
        assert backupStorageSpec.inventory.uuid


        attachIsoToVmInstance {
            isoUuid = imageSpec.inventory.uuid
            vmInstanceUuid = vmSpec.inventory.uuid
            sessionId =  currentEnvSpec.session.uuid
        }
        detachIsoFromVmInstance {
            vmInstanceUuid = vmSpec.inventory.uuid
            sessionId =  currentEnvSpec.session.uuid
        }

        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class)
        q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, imageSpec.inventory.uuid)
        q.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, primaryStorageSpec.inventory.uuid)
        ImageCacheVO cache = q.find()

        assert cache != null
        assert cache.installUrl != null

        PrimaryStorageVO  psvo = dbf.findByUuid(primaryStorageSpec.inventory.uuid,PrimaryStorageVO.class)
        psvo.state = PrimaryStorageState.Disabled
        dbf.updateAndRefresh(psvo)
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid,PrimaryStorageVO.class).state == PrimaryStorageState.Disabled


        env.simulator(CephPrimaryStorageBase.CHECK_BITS_PATH) {
            CephPrimaryStorageBase.CheckIsBitsExistingRsp rsp = new CephPrimaryStorageBase.CheckIsBitsExistingRsp()
            rsp.success = false
            return rsp
        }

        AttachIsoToVmInstanceAction a = new AttachIsoToVmInstanceAction()
        a.isoUuid = imageSpec.inventory.uuid
        a.vmInstanceUuid = vmSpec.inventory.uuid
        a.sessionId = currentEnvSpec.session.uuid

        assert dbf.findByUuid(backupStorageSpec.inventory.uuid,BackupStorageVO.class).status == BackupStorageStatus.Connected

        AttachIsoToVmInstanceAction.Result res = a.call()
        assert res.error != null : "fail to attach iso ${res.error}"

    }

}
