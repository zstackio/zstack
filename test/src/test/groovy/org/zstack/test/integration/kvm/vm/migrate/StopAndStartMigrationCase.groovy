package org.zstack.test.integration.kvm.vm.migrate

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmInstanceExtensionPointEmitter
import org.zstack.compute.vm.VmInstanceManagerImpl
import org.zstack.core.workflow.FlowChainBuilder
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.Platform
import org.zstack.header.core.Completion
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.core.workflow.NoRollbackFlow
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.allocator.DesignatedAllocateHostMsg
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.host.MigrateVmOnHypervisorMsg
import org.zstack.header.message.MessageReply
import org.zstack.header.vm.*
import org.zstack.header.vm.devices.*
import org.zstack.compute.vm.devices.VmInstanceDeviceManagerImpl
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil


class StopAndStartMigrationCase extends SubCase {
    static class CustomLiveMigrationFlow extends NoRollbackFlow {
        void run(FlowTrigger trigger, Map data) {
            trigger.fail(Platform.operr('The custom Live chain must not execute for StopAndStart'))
        }
    }

    EnvSpec env
    VmInstanceExtensionPointEmitter emitter
    List<VmInstanceMigrateExtensionPoint> originalMigrationExtensions
    List<String> migrationCallbacks = []
    FlowChainBuilder originalLiveBuilder
    VmInstanceDeviceManagerImpl deviceManager
    List<VmDeviceProvider> originalProviders
    String vmUuid, source, target, stopHost, startHost
    List<String> calls = []
    Map<String, String> states = [:]
    int allocations
    boolean failSourceStop, failStart

    @Override
    void setup() { useSpring(KvmTest.springSpec) }

    @Override
    void environment() { env = VmMigrateEnv.oneVmThreeHostsNfsStorage() }

    @Override
    void test() {
        env.create {
            vmUuid = env.inventoryByName('vm').uuid
            source = env.inventoryByName('kvm').uuid
            target = env.inventoryByName('kvm1').uuid
            states[source] = 'Running'
            states[target] = 'Stopped'
            emitter = bean(VmInstanceExtensionPointEmitter.class)
            originalMigrationExtensions = emitter.migrateVmExtensions
            emitter.migrateVmExtensions = originalMigrationExtensions + [new VmInstanceMigrateExtensionPoint() {
                void preMigrateVm(VmInstanceInventory inv, String host) { migrationCallbacks.add('pre') }
                void beforeMigrateVm(VmInstanceInventory inv, String host) { migrationCallbacks.add('before') }
                void postMigrateVm(VmInstanceInventory inv, String host) { migrationCallbacks.add('post') }
                void afterMigrateVm(VmInstanceInventory inv, String host) { migrationCallbacks.add('after') }
            }]
            deviceManager = bean(VmInstanceDeviceManagerImpl)
            originalProviders = deviceManager.deviceProviders
            def device = new VmDevice() {
                String getUuid() { 'test-device' }
                VmDeviceMigrationCapability getMigrationCapability() { VmDeviceMigrationCapability.StopAndStart }
                String getMigrationErrorCode() { 'TEST.LIVE_DEVICE' }
            }
            deviceManager.deviceProviders = originalProviders + [{ String uuid -> [device] } as VmDeviceProvider]
            installSimulators()
            def reply = migrate(target, VmMigrationMode.Live)
            assert !reply.success && reply.error.globalErrorCode == 'TEST.LIVE_DEVICE' :
                    "wrong rejection: ${reply.error}"
            assert calls.empty && allocations == 0 : 'live check must precede allocation'
            def immovable = new VmDevice() {
                String getUuid() { 'immovable-device' }
                VmDeviceMigrationCapability getMigrationCapability() { VmDeviceMigrationCapability.None }
                String getMigrationErrorCode() { 'TEST.IMMOVABLE_DEVICE' }
            }
            deviceManager.deviceProviders = originalProviders + [{ String uuid -> [immovable] } as VmDeviceProvider,
                    { String uuid -> throw new AssertionError('A rejected migration must not read later providers') }
                            as VmDeviceProvider]
            def rejected = migrate(target)
            assert !rejected.success && rejected.error.globalErrorCode == 'TEST.IMMOVABLE_DEVICE' :
                    'StopAndStart must reject devices whose declared capability permits neither mode'
            assert calls.empty && allocations == 0 : 'Device rejection must happen before reservation or source stop'
            deviceManager.deviceProviders = originalProviders + [{ String uuid -> [device] } as VmDeviceProvider]


            def manager = bean(VmInstanceManagerImpl.class)
            originalLiveBuilder = manager.migrateVmFlowBuilder
            manager.migrateVmFlowBuilder = FlowChainBuilder.newBuilder()
                    .setFlowClassNames([CustomLiveMigrationFlow.name]).construct()

            changeHostState { uuid = target; stateEvent = 'disable' }
            assert !migrate(target).success : 'disabled target must fail actual allocation'
            assert calls.empty : "no destination must leave source untouched: ${calls}"
            changeHostState { uuid = target; stateEvent = 'enable' }
            allocations = 0

            long before = memory(target)
            failSourceStop = true
            assert !migrate(target).success : 'source stop failure must fail migration'
            failSourceStop = false
            assert calls == ["stop:${source}".toString()] : "target must not start: ${calls}"
            assert currentVm().state == VmInstanceState.Running && currentVm().hostUuid == source :
                    'source must remain Running'
            retryInSecs { assert memory(target) == before : 'failed stop must return target capacity' }
            resetCounters()

            long sourceBefore = memory(source)
            long targetBefore = memory(target)
            long size = currentVm().memorySize
            reply = migrate(target)
            assert reply.success : "cold migration should honor the device restart capability: ${reply.error}"
            assert calls == ["stop:${source}".toString(), "start:${target}".toString()] : "wrong order: ${calls}"
            assert allocations == 1 : "cold migration must allocate once: ${allocations}"
            assert migrationCallbacks == ['pre', 'before', 'post', 'after'] :
                    'Extensions without a mode override must retain all migration callbacks in StopAndStart'
            manager.migrateVmFlowBuilder = originalLiveBuilder
            assert currentVm().hostUuid == target && currentVm().lastHostUuid == source :
                    'placement must move to target'
            assert currentVm().state == VmInstanceState.Running : 'successful cold start must be Running'
            retryInSecs {
                assert memory(source) == sourceBefore + size : 'source capacity must be returned once'
                assert memory(target) == targetBefore - size : 'target capacity must remain occupied once'
            }
            resetCounters()

            before = memory(source)
            failStart = true
            assert !migrate(source).success : 'start failure must fail migration'
            failStart = false
            assert calls == ["stop:${target}".toString(), "start:${source}".toString()] :
                    "a failed start leaves nothing to stop on the target: ${calls}"
            assert currentVm().state == VmInstanceState.Stopped :
                    "stopped source must not become Running: ${currentVm().state}"
            assert currentVm().hostUuid == null && currentVm().lastHostUuid == target :
                    'stopped VM must retain last source'
            retryInSecs { assert memory(source) == before : 'unused target capacity must be released' }

            startVmInstance { uuid = vmUuid; hostUuid = source }
            resetCounters()
            long targetCapacity = memory(target)
            def registry = bean(PluginRegistry)
            def refuseStart = new VmBeforeStartOnHypervisorExtensionPoint() {
                @Override
                void beforeStartVmOnHypervisor(VmInstanceSpec spec, Completion completion) {
                    completion.fail(Platform.operr('injected target preparation failure'))
                }
            }
            registry.defineDynamicExtension(VmBeforeStartOnHypervisorExtensionPoint, refuseStart)
            try {
                assert !migrate(target).success : 'Failed target preparation must fail the migration'
                assert calls == ["stop:${source}".toString()] : 'No target command means no target stop is needed'
                assert currentVm().state == VmInstanceState.Stopped && currentVm().hostUuid == null
                retryInSecs { assert memory(target) == targetCapacity : 'Unused target reservation must be released' }
            } finally {
                registry.getExtensionList(VmBeforeStartOnHypervisorExtensionPoint).remove(refuseStart)
            }
        }
    }

    void installSimulators() {
        notifyWhenReceivedMessage(DesignatedAllocateHostMsg.class) { allocations++ }
        notifyWhenReceivedMessage(StopVmOnHypervisorMsg.class) { StopVmOnHypervisorMsg message ->
            stopHost = message.hostUuid
            calls.add("stop:${stopHost}".toString())
        }
        notifyWhenReceivedMessage(StartVmOnHypervisorMsg.class) { StartVmOnHypervisorMsg message ->
            startHost = message.vmSpec.destHost.uuid
            assert currentVm().state != VmInstanceState.Migrating || currentVm().hostUuid != startHost :
                    'placement switches only after the target starts'
            calls.add("start:${startHost}".toString())
        }
        notifyWhenReceivedMessage(MigrateVmOnHypervisorMsg.class) { calls.add('live-migrate') }
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp ->
            if (failSourceStop) {
                rsp.success = false
                rsp.error = 'injected stop failure'
            } else { states[stopHost] = 'Stopped' }
            rsp
        }
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp ->
            if (failStart) {
                rsp.success = false
                rsp.error = 'injected start failure'
            } else { states[startHost] = 'Running' }
            rsp
        }
        env.simulator(KVMConstant.KVM_VM_CHECK_STATE) { HttpEntity<String> entity ->
            def cmd = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.CheckVmStateCmd.class)
            String state = states[cmd.hostUuid] == 'Running' ? 'Running' : 'Shutdown'
            new KVMAgentCommands.CheckVmStateRsp(states: [(vmUuid): state])
        }
    }

    MessageReply migrate(String host, VmMigrationMode mode = VmMigrationMode.StopAndStart) {
        def msg = new MigrateVmMsg(vmInstanceUuid: vmUuid, targetHostUuid: host, migrationMode: mode, timeout: 60000)
        CloudBus bus = bean(CloudBus.class)
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid)
        bus.call(msg)
    }

    VmInstanceVO currentVm() { dbFindByUuid(vmUuid, VmInstanceVO.class) }
    long memory(String host) { dbFindByUuid(host, HostCapacityVO.class).availableMemory }
    void resetCounters() { calls.clear(); allocations = 0; migrationCallbacks.clear() }

    @Override
    void clean() {
        failStart = false
        failSourceStop = false
        if (deviceManager != null && originalProviders != null) { deviceManager.deviceProviders = originalProviders }
        if (emitter != null && originalMigrationExtensions != null) {
            emitter.migrateVmExtensions = originalMigrationExtensions
        }
        if (originalLiveBuilder != null) { bean(VmInstanceManagerImpl).migrateVmFlowBuilder = originalLiveBuilder }
        env.delete()
    }
}
