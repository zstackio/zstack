package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.compute.vm.VmInstanceExtensionPointEmitter
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.vm.VmInstanceInventory as HeaderVmInventory
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.sdk.HostInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.operr

class MigrateVmFinalizeFailureCase extends SubCase {
    EnvSpec env
    VmInstanceExtensionPointEmitter emitter
    List<VmInstanceMigrateExtensionPoint> originalExtensions
    VmInstanceInventory vm
    HostInventory source
    HostInventory target
    int finalized
    int rolledBack
    boolean rejectPrepare
    boolean rejectFinalize = true
    boolean throwFinalize
    static final String FINALIZE_ERROR = "ZCF6115.TEST.FINALIZE"
    static final String PREPARE_ERROR = "ZCF6115.TEST.PREPARE"

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = VmMigrateEnv.oneVmThreeHostsNfsStorage()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            source = env.inventoryByName("kvm") as HostInventory
            target = env.inventoryByName("kvm1") as HostInventory
            emitter = bean(VmInstanceExtensionPointEmitter.class)
            originalExtensions = emitter.migrateVmExtensions
            def extension = new VmInstanceMigrateExtensionPoint() {
                @Override
                void preMigrateVm(HeaderVmInventory inventory, String destination, Completion completion) {
                    if (rejectPrepare) {
                        completion.fail(operr(PREPARE_ERROR, "migration preparation rejected by test"))
                    } else {
                        completion.success()
                    }
                }

                @Override
                void beforeMigrateVm(HeaderVmInventory inventory, String destination) {
                }

                void finalizeMigrateVm(HeaderVmInventory inventory, String sourceHostUuid, Completion completion) {
                    finalized++
                    assert sourceHostUuid == source.uuid
                    assert inventory.hostUuid == target.uuid
                    assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == target.uuid
                    if (throwFinalize) {
                        throw new IllegalStateException('synchronous finalization exception')
                    } else if (rejectFinalize) {
                        completion.fail(operr(FINALIZE_ERROR, "destination network is not realized"))
                    } else {
                        completion.success()
                    }
                }

                @Override
                void failedToMigrateVm(HeaderVmInventory inventory, String destination, ErrorCode reason) {
                    rolledBack++
                }
            }
            emitter.migrateVmExtensions = new ArrayList<>(originalExtensions)
            emitter.migrateVmExtensions.add(extension)
            testFinalizationFailurePreservesDestination()
            emitter.migrateVmExtensions = originalExtensions
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = source.uuid
            }
            emitter.migrateVmExtensions = new ArrayList<>(originalExtensions)
            emitter.migrateVmExtensions.add(extension)
            testPreparationFailureStillRollsBack()
            rejectPrepare = false
            rejectFinalize = false
            throwFinalize = true
            def exceptionError = migrate(target.uuid).error
            assert exceptionError?.code == 'SYS.1000'
            assert exceptionError.details.contains('synchronous finalization exception')
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == target.uuid
            assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Running
            assert rolledBack == 1
            emitter.migrateVmExtensions = originalExtensions
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = source.uuid
            }
            emitter.migrateVmExtensions = new ArrayList<>(originalExtensions)
            emitter.migrateVmExtensions.add(extension)
            throwFinalize = false
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = target.uuid
            }
            assert finalized == 3
            assert rolledBack == 1
        }
    }

    void testFinalizationFailurePreservesDestination() {
        long sourceMemory = dbFindByUuid(source.uuid, HostCapacityVO.class).availableMemory
        long targetMemory = dbFindByUuid(target.uuid, HostCapacityVO.class).availableMemory
        def result = migrate(target.uuid)
        assert result.error != null
        assert result.error.globalErrorCode == FINALIZE_ERROR
        assert finalized == 1
        assert rolledBack == 0
        VmInstanceVO actual = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert actual.state == VmInstanceState.Running
        assert actual.hostUuid == target.uuid
        assert actual.lastHostUuid == source.uuid
        assert actual.clusterUuid == target.clusterUuid
        assert actual.zoneUuid == target.zoneUuid
        retryInSecs {
            assert dbFindByUuid(source.uuid, HostCapacityVO.class).availableMemory == sourceMemory + vm.memorySize
            assert dbFindByUuid(target.uuid, HostCapacityVO.class).availableMemory == targetMemory - vm.memorySize
        }
    }

    void testPreparationFailureStillRollsBack() {
        long sourceMemory = dbFindByUuid(source.uuid, HostCapacityVO.class).availableMemory
        long targetMemory = dbFindByUuid(target.uuid, HostCapacityVO.class).availableMemory
        rejectPrepare = true
        def result = migrate(target.uuid)
        assert result.error != null
        assert result.error.globalErrorCode == PREPARE_ERROR
        assert finalized == 1
        assert rolledBack == 1
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid == source.uuid
        retryInSecs {
            assert dbFindByUuid(source.uuid, HostCapacityVO.class).availableMemory == sourceMemory
            assert dbFindByUuid(target.uuid, HostCapacityVO.class).availableMemory == targetMemory
        }
    }

    MigrateVmAction.Result migrate(String destination) {
        def action = new MigrateVmAction()
        action.vmInstanceUuid = vm.uuid
        action.hostUuid = destination
        action.sessionId = adminSession()
        return action.call()
    }

    @Override
    void clean() {
        if (emitter != null && originalExtensions != null) {
            emitter.migrateVmExtensions = originalExtensions
        }
        env.delete()
    }
}
