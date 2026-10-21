package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.CephGlobalConfig
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.primary.PrimaryStorageDeleteBitGC
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.test.integration.storage.volume.VolumeEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2018/1/22.
 */
class CreateCephDataVolumeFromTemplateFailedCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    KVMHostInventory kvm
    ImageInventory image
    CephPrimaryStorageBase.CpCmd copyCommand

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
        env = VolumeEnv.cephStorageCephEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            try {
                createVolumeFailed(0)
                CephGlobalConfig.RBD_CONCURRENT_MANAGEMENT_OPS.updateValue(4)
                createVolumeFailed(4)
                [1, 0].each { limit ->
                    updateResourceConfig {
                        category = "ceph"
                        name = "rbd.concurrentManagementOps"
                        resourceUuid = ps.uuid
                        value = limit.toString()
                    }
                    createVolumeFailed(limit)
                }
                deleteResourceConfig {
                    category = "ceph"
                    name = "rbd.concurrentManagementOps"
                    resourceUuid = ps.uuid
                }
                createVolumeFailed(4)
            } finally {
                CephGlobalConfig.RBD_CONCURRENT_MANAGEMENT_OPS.updateValue(0)
            }
        }
    }

    void prepare() {
        ps = env.inventoryByName("ceph-ps") as PrimaryStorageInventory
        kvm = env.inventoryByName("kvm") as KVMHostInventory
        image = env.inventoryByName("image-data-volume") as ImageInventory

        env.simulator(CephPrimaryStorageBase.CP_PATH) { HttpEntity<String> e ->
            copyCommand = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CpCmd.class)
            def rsp = new CephPrimaryStorageBase.CpRsp()
            rsp.error = "test-failed"
            rsp.success = false
            return rsp
        }
    }

    void createVolumeFailed(int expectedConcurrency) {
        assert PrimaryStorageGlobalConfig.PRIMARY_STORAGE_DELETEBITS_ON.value() == "true"

        String volumeUuid = Platform.uuid

        String gcName = String.format("gc-delete-bits-volume-%s-on-primary-storage-%s", volumeUuid, ps.uuid)
        logger.debug(gcName)
        List<GarbageCollectorInventory> gcJobs = queryGCJob {
            conditions = ["name=${gcName}"]
        }

        assert gcJobs.size() == 0

        def action = new CreateDataVolumeFromVolumeTemplateAction()
        action.name = "volume"
        action.resourceUuid = volumeUuid
        action.imageUuid = image.uuid
        action.primaryStorageUuid = ps.uuid
        action.hostUuid = kvm.uuid
        action.sessionId = adminSession()
        copyCommand = null
        def result = action.call()
        assert result.error != null
        assert copyCommand?.rbdConcurrentManagementOps == expectedConcurrency :
                "CP must use the effective PS RBD concurrency: expected=${expectedConcurrency}, actual=${copyCommand?.rbdConcurrentManagementOps}"

        gcJobs = queryGCJob {
            conditions = ["name=${gcName}"]
        }
        assert gcJobs.size() == 1
        PrimaryStorageDeleteBitGC psGC = JSONObjectUtil.toObject(gcJobs.get(0).getContext(), PrimaryStorageDeleteBitGC.class)
        assert psGC.primaryStorageUuid == ps.uuid
        assert psGC.volume.uuid == volumeUuid
        // CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL.getTokenByResourceUuid(self.getUuid(), CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN)
        String pool = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL.
                getTokenByResourceUuid(ps.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN)
        assert psGC.primaryStorageInstallPath == String.format("ceph://%s/%s", pool, volumeUuid)
    }
}
