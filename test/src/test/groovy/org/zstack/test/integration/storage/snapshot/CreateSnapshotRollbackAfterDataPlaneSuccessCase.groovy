package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.core.workflow.Flow
import org.zstack.header.storage.snapshot.ConsistentType
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotOverlayInnerMsg
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmReply
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant
import org.zstack.header.storage.snapshot.VolumeSnapshotCreationExtensionPoint
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceVO
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceVO_
import org.zstack.header.volume.CreateVolumeSnapshotGroupMessage
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class CreateSnapshotRollbackAfterDataPlaneSuccessCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm

    int takeSnapshotCount = 0
    String firstSnapshotInstallPath
    String firstNewVolumeInstallPath
    String secondVolumeInstallPath
    String secondSnapshotInstallPath
    String secondNewVolumeInstallPath
    long snapshotSize = 1L

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
            testKeepSnapshotMetadataWhenControlPlaneFailsAfterDataPlaneSuccess()
        }
    }

    void testKeepSnapshotMetadataWhenControlPlaneFailsAfterDataPlaneSuccess() {
        String originalVolumeInstallPath = Q.New(VolumeVO.class)
                .select(VolumeVO_.installPath)
                .eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                .findValue()

        env.simulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.TakeSnapshotCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.TakeSnapshotCmd.class)
            takeSnapshotCount++

            if (takeSnapshotCount == 1) {
                assert cmd.volumeInstallPath == originalVolumeInstallPath
                firstSnapshotInstallPath = cmd.volumeInstallPath
                firstNewVolumeInstallPath = cmd.installPath
            } else if (takeSnapshotCount == 2) {
                secondVolumeInstallPath = cmd.volumeInstallPath
                secondSnapshotInstallPath = cmd.volumeInstallPath
                secondNewVolumeInstallPath = cmd.installPath
            }

            KVMAgentCommands.TakeSnapshotResponse rsp = new KVMAgentCommands.TakeSnapshotResponse()
            rsp.newVolumeInstallPath = cmd.installPath
            rsp.snapshotInstallPath = cmd.volumeInstallPath
            rsp.size = snapshotSize
            return rsp
        }

        PluginRegistry pluginRegistry = bean(PluginRegistry.class)
        List<VolumeSnapshotCreationExtensionPoint> extensions = pluginRegistry.getExtensionList(VolumeSnapshotCreationExtensionPoint.class)
        FailAfterSnapshotCreatedExtension failureExtension = new FailAfterSnapshotCreatedExtension()
        extensions.add(failureExtension)

        try {
            expectError {
                createVolumeSnapshot {
                    name = "snapshot-fail-after-metadata-synced"
                    volumeUuid = vm.rootVolumeUuid
                }
            }

            assert takeSnapshotCount == 1

            VolumeSnapshotVO firstSnapshot
            retryInSecs(10, 1) {
                firstSnapshot = Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.volumeUuid, vm.rootVolumeUuid)
                        .find()
                assert firstSnapshot != null
                assert firstSnapshot.status == VolumeSnapshotStatus.Ready
            }

            assert firstSnapshot.latest
            assert firstSnapshot.primaryStorageInstallPath == firstSnapshotInstallPath
            assert firstSnapshot.type == VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString()
            assert firstSnapshot.size == snapshotSize

            VolumeSnapshotTreeVO firstTree = dbFindByUuid(firstSnapshot.treeUuid, VolumeSnapshotTreeVO.class)
            assert firstTree.current
            assert firstTree.status == VolumeSnapshotTreeStatus.Completed

            assert Q.New(VolumeVO.class)
                    .select(VolumeVO_.installPath)
                    .eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                    .findValue() == firstNewVolumeInstallPath

            VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                    .eq(VolumeSnapshotReferenceVO_.volumeUuid, vm.rootVolumeUuid)
                    .limit(1)
                    .find()
            if (ref != null && ref.referenceInstallUrl == firstSnapshotInstallPath) {
                assert ref.referenceUuid == firstSnapshot.uuid
                assert ref.referenceType == VolumeSnapshotVO.class.simpleName
            }

            org.zstack.sdk.VolumeSnapshotInventory secondSnapshotInv = createVolumeSnapshot {
                name = "snapshot-after-recovery"
                volumeUuid = vm.rootVolumeUuid
            } as org.zstack.sdk.VolumeSnapshotInventory

            assert takeSnapshotCount == 2
            assert secondVolumeInstallPath == firstNewVolumeInstallPath

            VolumeSnapshotVO firstSnapshotAfterRecovery = dbFindByUuid(firstSnapshot.uuid, VolumeSnapshotVO.class)
            VolumeSnapshotVO secondSnapshot = dbFindByUuid(secondSnapshotInv.uuid, VolumeSnapshotVO.class)
            assert !firstSnapshotAfterRecovery.latest
            assert secondSnapshot.latest
            assert secondSnapshot.parentUuid == firstSnapshot.uuid
            assert secondSnapshot.primaryStorageInstallPath == secondSnapshotInstallPath
            assert secondSnapshot.status == VolumeSnapshotStatus.Ready

            assert Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.volumeUuid, vm.rootVolumeUuid)
                    .count() == 2
            assert Q.New(VolumeVO.class)
                    .select(VolumeVO_.installPath)
                    .eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                    .findValue() == secondNewVolumeInstallPath
        } finally {
            extensions.remove(failureExtension)
        }
    }

    private static class FailAfterSnapshotCreatedExtension implements VolumeSnapshotCreationExtensionPoint {
        boolean failAfterSnapshotCreated = true

        @Override
        void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
            if (failAfterSnapshotCreated) {
                failAfterSnapshotCreated = false
                completion.fail(Platform.operr("TEST.ERROR", "fail after KVM snapshot has been created"))
                return
            }

            completion.success()
        }

        @Override
        void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply, Completion completion) {
            completion.success()
        }

        @Override
        void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg, TakeVolumesSnapshotOnKvmReply treply) {
        }

        @Override
        void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup, ConsistentType consistentType, Completion completion) {
            completion.success()
        }

        @Override
        List<Flow> beforeCreateVolumeSnapshotFlow(CreateVolumeSnapshotGroupMessage msg) {
            return null
        }
    }
}
