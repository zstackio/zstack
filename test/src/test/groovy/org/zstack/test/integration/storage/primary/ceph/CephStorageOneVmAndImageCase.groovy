package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.volume.VolumeConstant
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AddCephPrimaryStorageAction
import org.zstack.sdk.CephPrimaryStorageInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by heathhose on 17-3-22.
 */
class CephStorageOneVmAndImageCase extends SubCase{
    def description = """
        1. use ceph for primary storage and backup storage
        2. create a vm
        3. create an image from the vm's root volume
        confirm the volume created successfully
    """

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
            testCurrentVolumeConfigurationAfterMonReplacement()
            createImageFromRootVolume()
            testCreateWithNoCephx()
            testSyncVolumeActualSize()
        }
    }

    void createImageFromRootVolume(){
        VmInstanceInventory vm = env.inventoryByName("test-vm")
        stopVmInstance {
            uuid = vm.uuid
            sessionId = loginAsAdmin().uuid
        }

        long count = Q.New(TaskProgressVO.class).count()
        ImageInventory img = createRootVolumeTemplateFromRootVolume {
            name = "template"
            rootVolumeUuid = vm.getRootVolumeUuid()
            sessionId = loginAsAdmin().uuid
        }

        assert count < Q.New(TaskProgressVO.class).count()
        assert VolumeConstant.VOLUME_FORMAT_RAW == img.getFormat()
    }

    void testCreateWithNoCephx() {
        def fsid = "8ff218d9-f525-435f-8a40-3618d1772a64"
        def nocephx = false
        env.simulator(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.GetFactsRsp()
            rsp.fsid = fsid
            rsp.monAddr = "127.0.0.2"
            rsp.success = true
            return rsp
        }

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            nocephx = cmd.nocephx

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.success = true
            rsp.fsid = fsid
            rsp.totalCapacity = 400000000
            rsp.availableCapacity = 400000000
            return rsp
        }

        AddCephPrimaryStorageAction action = new AddCephPrimaryStorageAction()
        action.name = "ceph-primary-new"
        action.monUrls = ["root:password@127.0.0.2"]
        action.rootVolumePoolName = "rootPool"
        action.dataVolumePoolName = "dataPool"
        action.imageCachePoolName = "cachePool"
        action.systemTags = [ "ceph::nocephx" ]
        action.zoneUuid = env.inventoryByName("zone").uuid
        action.sessionId = adminSession()
        AddCephPrimaryStorageAction.Result res = action.call()
        assert res != null
        assert nocephx
        assert res.error == null
    }

    void testSyncVolumeActualSize(){
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory
        env.simulator(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) {
            def rsp = new CephPrimaryStorageBase.GetVolumeSizeRsp()
            rsp.size = 0
            return rsp
        }

        env.simulator(CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CreateSnapshotCmd.class)
            def rsp = new CephPrimaryStorageBase.CreateSnapshotRsp()
            rsp.size = 0
            rsp.installPath = cmd.snapshotPath
            return rsp
        }

        createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "test_snap"
        }

        def vol1 = syncVolumeSize {
            uuid = vm.rootVolumeUuid
        } as VolumeInventory

        def vol2 = syncVolumeSize {
            uuid = vm.rootVolumeUuid
        } as VolumeInventory

        assert vol1.actualSize == vol2.actualSize
    }
    
    void testCurrentVolumeConfigurationAfterMonReplacement() {
        VmInstanceInventory vm = env.inventoryByName("test-vm")
        CephPrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        HostInventory destination = env.inventoryByName("ceph-mon")
        String sourceHostUuid = vm.hostUuid
        VolumeInventory data = createDataVolume {
            name = "migration-data"
            diskOfferingUuid = env.inventoryByName("diskOffering").uuid
            primaryStorageUuid = ps.uuid
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = data.uuid
        }

        addMonToCephPrimaryStorage {
            uuid = ps.uuid
            monUrls = ["root:password@127.0.0.3/?monPort=7788"]
        }
        removeMonFromCephPrimaryStorage {
            uuid = ps.uuid
            monHostnames = ["localhost"]
        }

        Map command = null
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> request ->
            command = JSONObjectUtil.toObject(request.body, LinkedHashMap.class)
            return rsp
        }
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = destination.uuid
        }
        String secretUuid = CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(ps.uuid, CephSystemTags.KVM_SECRET_UUID_TOKEN)
        assert secretUuid : "Ceph storage must have a secret UUID: expected=non-empty actual=${secretUuid}"
        assertVolumeConfiguration(command, [vm.rootVolumeUuid, data.uuid] as Set, 7788, secretUuid)

        def noCephxTag = createSystemTag {
            resourceType = PrimaryStorageVO.class.simpleName
            resourceUuid = ps.uuid
            tag = CephSystemTags.NO_CEPHX_TOKEN
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        reconnectHost {
            uuid = sourceHostUuid
        }
        command = null
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = sourceHostUuid
        }
        assertVolumeConfiguration(command, [vm.rootVolumeUuid, data.uuid] as Set, 7788, null)

        deleteTag {
            uuid = noCephxTag.uuid
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        reconnectHost {
            uuid = destination.uuid
        }
        command = null
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = destination.uuid
        }
        assertVolumeConfiguration(command, [vm.rootVolumeUuid, data.uuid] as Set, 7788, secretUuid)

        ps = queryCephPrimaryStorage { conditions = ["uuid=${ps.uuid}"] }[0]
        updateCephPrimaryStorageMon {
            monUuid = ps.mons[0].monUuid
            monPort = 7799
        }
        detachDataVolumeFromVm {
            uuid = data.uuid
        }
        command = null
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = sourceHostUuid
        }
        assertVolumeConfiguration(command, [vm.rootVolumeUuid] as Set, 7799, secretUuid)
    }

    private void assertVolumeConfiguration(Map command, Set expectedVolumes, int expectedPort, String expectedSecretUuid) {
        assert command != null : "Migration must send a command: expected=non-null actual=${command}"
        Set actualVolumes = command.notMigrateDisks?.values()?.collect { it.volumeUuid } as Set
        assert actualVolumes == expectedVolumes :
                "Migration must refresh only attached volumes: expected=${expectedVolumes} actual=${actualVolumes}"
        assert !command.disks :
                "Shared volumes must not request storage copying: expected=empty actual=${command.disks}"
        command.notMigrateDisks.each { installPath, volume ->
            assert installPath == volume.installPath
            assert volume.secretUuid == expectedSecretUuid :
                    "Volume ${volume.volumeUuid} must follow current cephx configuration: expected=${expectedSecretUuid} actual=${volume.secretUuid}"
            def endpoints = volume.monInfo.collect { "${it.hostname}:${it.port.intValue()}" }
            assert endpoints == ["127.0.0.3:${expectedPort}"] :
                    "Volume ${volume.volumeUuid} must use current MON configuration: expected=127.0.0.3:${expectedPort} actual=${endpoints}"
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
