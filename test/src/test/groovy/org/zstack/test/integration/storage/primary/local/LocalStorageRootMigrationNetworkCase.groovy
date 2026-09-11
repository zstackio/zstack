package org.zstack.test.integration.storage.primary.local

import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.vm.VmInstanceInventory as HeaderVmInventory
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.APIStartVmInstanceMsg
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeInventory
import org.zstack.sdk.LocalStorageMigrateVolumeAction
import org.zstack.sdk.StartVmInstanceAction
import org.zstack.kvm.KVMConstant
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.storage.primary.local.LocalStorageBase
import org.zstack.storage.primary.local.LocalStorageResourceRefVO
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_
import org.zstack.storage.primary.local.LocalStorageRootVolumeMigrationExtensionPoint
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.operr
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocalStorageRootMigrationNetworkCase extends SubCase {
    EnvSpec env
    def vm
    def source
    def target
    LocalStorageRootVolumeMigrationExtensionPoint extension
    boolean rejectPrepare = true
    boolean rejectFinalize = true
    boolean throwFinalize
    int prepared
    int finalized
    int copies
    boolean holdFinalize
    Completion heldCompletion
    CountDownLatch networkEntered = new CountDownLatch(1)

    @Override
    void setup() { useSpring(StorageTest.springSpec) }

    @Override
    void environment() { env = Env.localStorageOneVmEnv() }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName('vm')
            source = env.inventoryByName('kvm')
            target = env.inventoryByName('kvm1')
            stopVmInstance { uuid = vm.uuid }
            verifyRootExtensionFirstCompletion()
            extension = new LocalStorageRootVolumeMigrationExtensionPoint() {
                @Override
                void preMigrateRootVolume(HeaderVmInventory inventory, String from, String to, Completion completion) {
                    prepared++
                    assert inventory.uuid == vm.uuid
                    assert inventory.state == VmInstanceState.VolumeMigrating.toString()
                    assert rootHost() == from
                    if (rejectPrepare) completion.fail(operr('ZCF6115.TEST.COLD_PREPARE', 'target network rejected'))
                    else completion.success()
                }

                @Override
                void finalizeMigrateRootVolume(HeaderVmInventory inventory, String from, String to, Completion completion) {
                    finalized++
                    assert copies > 0
                    assert rootHost() == to
                    assert dbFindByUuid(vm.uuid, VmInstanceVO).lastHostUuid == to
                    assert inventory.state == VmInstanceState.VolumeMigrating.toString()
                    if (holdFinalize) {
                        heldCompletion = completion
                        networkEntered.countDown()
                        return
                    }
                    if (throwFinalize) throw new IllegalStateException('synchronous network extension exception')
                    if (rejectFinalize) completion.fail(operr('ZCF6115.TEST.COLD_FINALIZE', 'port placement rejected'))
                    else completion.success()
                }
            }
            bean(PluginRegistry).defineDynamicExtension(LocalStorageRootVolumeMigrationExtensionPoint, extension)
            env.afterSimulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) { response ->
                copies++
                response
            }
            def dataVolume = createDataVolume {
                name = 'network-extension-independent-data'
                diskOfferingUuid = env.inventoryByName('diskOffering').uuid
            }
            try {
                attachDataVolumeToVm { vmInstanceUuid = vm.uuid; volumeUuid = dataVolume.uuid }
                detachDataVolumeFromVm { uuid = dataVolume.uuid }
                def dataMigration = new LocalStorageMigrateVolumeAction(volumeUuid: dataVolume.uuid,
                        destHostUuid: target.uuid, sessionId: adminSession()).call()
                assert dataMigration.error == null
                assert copies > 0 && prepared == 0 && finalized == 0
                assert Q.New(LocalStorageResourceRefVO).eq(LocalStorageResourceRefVO_.resourceUuid, dataVolume.uuid)
                        .select(LocalStorageResourceRefVO_.hostUuid).findValue() == target.uuid
            } finally {
                deleteDataVolume { uuid = dataVolume.uuid }
                expungeDataVolume { uuid = dataVolume.uuid }
            }
            copies = 0
            def rejected = migrate(target.uuid)
            assert rejected.error?.globalErrorCode == 'ZCF6115.TEST.COLD_PREPARE'
            assert prepared == 1 && finalized == 0 && copies == 0
            assert rootHost() == source.uuid
            assertStoppedAndReady()

            rejectPrepare = false
            def networkFailure = migrate(target.uuid)
            assert networkFailure.error?.globalErrorCode == 'ZCF6115.TEST.COLD_FINALIZE'
            assert finalized == 1
            assert rootHost() == target.uuid
            assert dbFindByUuid(vm.uuid, VmInstanceVO).lastHostUuid == target.uuid
            assertStoppedAndReady()
            def started = startVmInstance { uuid = vm.uuid }
            assert started.hostUuid == target.uuid
            stopVmInstance { uuid = vm.uuid }
            rejectFinalize = false
            throwFinalize = true
            def exceptionError = migrate(source.uuid).error
            assert exceptionError?.code == 'SYS.1000'
            assert exceptionError.details.contains('synchronous network extension exception')
            assert rootHost() == source.uuid
            assertStoppedAndReady()
            throwFinalize = false
            assert migrate(target.uuid).error == null
            assert prepared == 4 && finalized == 3
            assert rootHost() == target.uuid
            assertStoppedAndReady()
            verifyExistingSerializationWhileNetworkPending()
        }
    }

    private void verifyExistingSerializationWhileNetworkPending() {
        List results = Collections.synchronizedList([])
        CountDownLatch finished = new CountDownLatch(3)
        CountDownLatch admission = new CountDownLatch(2)
        Set<Class> seen = Collections.synchronizedSet(new HashSet<Class>())
        int starts = 0
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { response -> starts++; response }
        holdFinalize = true
        new LocalStorageMigrateVolumeAction(volumeUuid: vm.rootVolumeUuid, destHostUuid: source.uuid,
                sessionId: adminSession()).call { result -> results.add(result); finished.countDown() }
        assert networkEntered.await(30, TimeUnit.SECONDS)
        int copied = copies
        bean(CloudBus).installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            void beforeDeliveryMessage(Message message) {
                if (holdFinalize && seen.add(message.class)) admission.countDown()
            }
        }, APIStartVmInstanceMsg, APILocalStorageMigrateVolumeMsg)
        try {
            new StartVmInstanceAction(uuid: vm.uuid, sessionId: adminSession()).call {
                result -> results.add(result); finished.countDown()
            }
            new LocalStorageMigrateVolumeAction(volumeUuid: vm.rootVolumeUuid, destHostUuid: target.uuid,
                    sessionId: adminSession()).call { result -> results.add(result); finished.countDown() }
            assert admission.await(10, TimeUnit.SECONDS)
            assert dbFindByUuid(vm.uuid, VmInstanceVO).state == VmInstanceState.VolumeMigrating
            assert rootHost() == source.uuid && copies == copied && starts == 0
        } finally {
            holdFinalize = false
            Completion callback = heldCompletion
            heldCompletion = null
            callback.success()
        }
        assert finished.await(30, TimeUnit.SECONDS): 'existing VM/volume queues must finish after network completion'
        assert results.size() == 3
        assert dbFindByUuid(vm.uuid, VmInstanceVO).state != VmInstanceState.VolumeMigrating
        assert dbFindByUuid(vm.rootVolumeUuid, VolumeVO).status == VolumeStatus.Ready
    }

    private void verifyRootExtensionFirstCompletion() {
        [false, true].each { boolean finalizing ->
            [false, true].each { boolean failFirst ->
                ErrorCode original = operr('ZCF6115.TEST.FIRST_COMPLETION', 'first completion')
                ErrorCode late = operr('ZCF6115.TEST.LATE_COMPLETION', 'late completion')
                Completion pending
                List<ErrorCode> results = []
                int nextCalls = 0
                Closure firstCall = { Completion completion ->
                    if (failFirst) {
                        completion.fail(original)
                        completion.success()
                    } else {
                        completion.success()
                        throw new OperationFailureException(late)
                    }
                }
                def first = new LocalStorageRootVolumeMigrationExtensionPoint() {
                    void preMigrateRootVolume(HeaderVmInventory inventory, String from, String to, Completion completion) {
                        firstCall(completion)
                    }
                    void finalizeMigrateRootVolume(HeaderVmInventory inventory, String from, String to, Completion completion) {
                        firstCall(completion)
                    }
                }
                def second = new LocalStorageRootVolumeMigrationExtensionPoint() {
                    void preMigrateRootVolume(HeaderVmInventory inventory, String from, String to, Completion completion) {
                        nextCalls++
                        pending = completion
                    }
                    void finalizeMigrateRootVolume(HeaderVmInventory inventory, String from, String to, Completion completion) {
                        nextCalls++
                        pending = completion
                    }
                }
                def registry = mock(PluginRegistry)
                when(registry.getExtensionList(LocalStorageRootVolumeMigrationExtensionPoint)).thenReturn([first, second])
                def storage = new LocalStorageBase()
                storage.pluginRgty = registry
                storage.callRootVolumeMigrationExtensions(
                        VolumeInventory.valueOf(dbFindByUuid(vm.rootVolumeUuid, VolumeVO)),
                        source.uuid, target.uuid, finalizing, new Completion(null) {
                    void success() { results.add(null) }
                    void fail(ErrorCode error) { results.add(error) }
                })
                if (failFirst && !finalizing) {
                    assert nextCalls == 0
                    assert pending == null
                } else {
                    assert nextCalls == 1
                    assert pending != null
                    assert results.empty
                    pending.success()
                }
                assert results.size() == 1
                assert results[0].is(failFirst ? original : null)
            }
        }
    }

    private def migrate(String targetUuid) {
        new LocalStorageMigrateVolumeAction(volumeUuid: vm.rootVolumeUuid,
                destHostUuid: targetUuid, sessionId: adminSession()).call()
    }

    private String rootHost() {
        Q.New(LocalStorageResourceRefVO).eq(LocalStorageResourceRefVO_.resourceUuid, vm.rootVolumeUuid)
                .select(LocalStorageResourceRefVO_.hostUuid).findValue()
    }

    private void assertStoppedAndReady() {
        assert dbFindByUuid(vm.uuid, VmInstanceVO).state == VmInstanceState.Stopped
        assert dbFindByUuid(vm.rootVolumeUuid, VolumeVO).status == VolumeStatus.Ready
    }

    @Override
    void clean() {
        if (extension != null) bean(PluginRegistry).getExtensionList(LocalStorageRootVolumeMigrationExtensionPoint).remove(extension)
        env.delete()
    }
}
