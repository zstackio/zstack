package org.zstack.test.integration.kvm.capacity

import com.google.common.base.Ticker
import org.apache.logging.log4j.ThreadContext
import org.zstack.header.Constants
import org.zstack.compute.allocator.HostAllocatorManagerImpl
import org.zstack.compute.allocator.HostCapacityUpdater
import org.zstack.compute.allocator.HostCapacityUpdaterRunnable
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CanonicalEvent
import org.zstack.core.cloudbus.EventFacadeImpl
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.Q
import org.zstack.header.allocator.HostCapacityOverProvisioningManager
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.allocator.HostCpuOverProvisioningManager
import org.zstack.header.host.RecalculateHostCapacityMsg
import org.zstack.header.vm.VmCanonicalEvents
import org.zstack.header.vm.VmCanonicalEvents.MigrationHostCapacityData
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmSchedHistoryVO
import org.zstack.header.vm.VmSchedHistoryVO_
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.HostInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import static org.zstack.kvm.KVMConstant.KVM_MIGRATE_VM_PATH

class RecalculateHostCapacityKeepInflightReserveCase extends SubCase {
    EnvSpec env
    HostAllocatorManagerImpl allocator
    EventFacadeImpl events
    VmInstanceInventory vm
    HostInventory source
    HostInventory target
    Map<String, String> migrationTaskIds = new ConcurrentHashMap<>()

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "src"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(64)
                    }
                    kvm {
                        name = "dst"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(64)
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("src")
            }
        }
    }

    @Override
    void test() {
        env.create {
            allocator = bean(HostAllocatorManagerImpl.class)
            events = bean(EventFacadeImpl.class)
            vm = env.inventoryByName("vm")
            source = env.inventoryByName("src")
            target = env.inventoryByName("dst")
            def extensions = bean(PluginRegistry.class).getExtensionList(VmInstanceMigrateExtensionPoint.class)
            assert extensions.contains(allocator) : "Migration cache must be registered: actual=${extensions}"
            KVMGlobalConfig.MIGRATE_AUTO_CONVERGE.updateValue(false)
            testMigrationCapacity(true)
            testMigrationCapacity(false)
            testOverprovisionedMigrationCapacity()
            testConcurrentMigrations()
            testRemoteEventsAndMemoryRatio()
            testInternalTaskContext()
            testExpiredMigrationCapacity()
        }
    }

    void testMigrationCapacity(boolean succeed, int sourceMemoryRatio = 1, int targetMemoryRatio = 1) {
        HostInventory from = succeed ? source : target
        HostInventory to = succeed ? target : source
        HostCapacityVO sourceBefore = dbFindByUuid(from.uuid, HostCapacityVO.class)
        HostCapacityVO targetBefore = dbFindByUuid(to.uuid, HostCapacityVO.class)
        long targetMemory = vm.memorySize.intdiv(targetMemoryRatio)
        long sourceMemory = vm.memorySize.intdiv(sourceMemoryRatio)
        long memory = targetBefore.availableMemory - targetMemory
        long cpu = targetBefore.availableCpu - vm.cpuNum
        boolean reachedAgent = false
        env.afterSimulator(KVM_MIGRATE_VM_PATH) { rsp ->
            reachedAgent = true
            VmInstanceVO actual = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert actual.state == VmInstanceState.Migrating : "VM state: expected=Migrating actual=${actual.state}"
            assert actual.hostUuid == from.uuid : "In-flight source: expected=${from.uuid} actual=${actual.hostUuid}"
            assert cache()[vm.uuid + ":" + migrationTaskIds[vm.uuid]] != null :
                    "Migration reserve must reuse the SDK task ID"
            assertNoHistory(vm.uuid)
            new HostCapacityUpdater(to.uuid).run({ HostCapacityVO cap ->
                cap.availableMemory = memory - 1
                cap.availableCpu = cpu - 1
                return cap
            } as HostCapacityUpdaterRunnable)
            [["hostUuid", to.uuid], ["hostUuid", from.uuid],
             ["clusterUuid", to.clusterUuid], ["zoneUuid", to.zoneUuid]].each { scope ->
                recalculate(scope[0], scope[1])
                assertCapacity(from.uuid, sourceBefore.availableMemory, sourceBefore.availableCpu, "source allocation")
                assertCapacity(to.uuid, memory, cpu, "${scope[0]} recalculation repairs drift and retains reservation")
            }
            if (!succeed) {
                rsp.error = "injected migration failure"
            }
            return rsp
        }
        def result = migrate(vm.uuid, to.uuid)
        assert reachedAgent : "SDK migration must reach agent: expected=true actual=${reachedAgent}"
        assert (result.error == null) == succeed : "Migration result: expectedSuccess=${succeed} actual=${result.error}"
        assert !hasReservation(vm.uuid) : "Completed migration must release target capacity"
        String hostUuid = dbFindByUuid(vm.uuid, VmInstanceVO.class).hostUuid
        assert hostUuid == (succeed ? to.uuid : from.uuid) :
                "Settled VM host: expected=${succeed ? to.uuid : from.uuid} actual=${hostUuid}"
        recalculate("clusterUuid", to.clusterUuid)
        assertCapacity(to.uuid, succeed ? memory : targetBefore.availableMemory,
                succeed ? cpu : targetBefore.availableCpu, "settled migration must not retain pending allocation")
        assertCapacity(from.uuid, sourceBefore.availableMemory + (succeed ? sourceMemory : 0),
                sourceBefore.availableCpu + (succeed ? vm.cpuNum : 0), "source capacity after migration")
        assertNoHistory(vm.uuid)
        env.afterSimulator(KVM_MIGRATE_VM_PATH) { rsp -> rsp }
    }

    void testOverprovisionedMigrationCapacity() {
        def reset = migrate(vm.uuid, source.uuid)
        assert reset.error == null : "Reset before overprovisioning: expected=success actual=${reset.error}"
        HostCapacityOverProvisioningManager memoryRatios = bean(HostCapacityOverProvisioningManager.class)
        HostCpuOverProvisioningManager cpuRatios = bean(HostCpuOverProvisioningManager.class)
        def hosts = [source, target]
        Map oldMemory = hosts.collectEntries { [(it.uuid): memoryRatios.allMemoryRatio[it.uuid]] }
        Map oldCpu = hosts.collectEntries { [(it.uuid): cpuRatios.allRatio[it.uuid]] }
        try {
            memoryRatios.setMemoryRatio(source.uuid, 4)
            memoryRatios.setMemoryRatio(target.uuid, 2)
            cpuRatios.setRatio(source.uuid, 2)
            cpuRatios.setRatio(target.uuid, 4)
            recalculate("clusterUuid", source.clusterUuid)
            HostCapacityVO src = dbFindByUuid(source.uuid, HostCapacityVO.class)
            HostCapacityVO dst = dbFindByUuid(target.uuid, HostCapacityVO.class)
            assert src.totalCpu == src.cpuNum * 2 :
                    "Source CPU total must be overprovisioned: expected=${src.cpuNum * 2} actual=${src.totalCpu}"
            assert dst.totalCpu == dst.cpuNum * 4 :
                    "Target CPU total must be overprovisioned: expected=${dst.cpuNum * 4} actual=${dst.totalCpu}"
            assertCapacity(source.uuid, src.totalMemory - vm.memorySize.intdiv(4), src.cpuNum * 2 - vm.cpuNum,
                    "source resident VM uses source memory ratio and unscaled vCPU count")
            assertCapacity(target.uuid, dst.totalMemory, dst.cpuNum * 4, "empty target uses its own CPU ratio")
            testMigrationCapacity(true, 4, 2)
            testMigrationCapacity(false, 2, 4)
            assertCapacity(source.uuid, src.totalMemory, src.cpuNum * 2, "failed return migration restores source")
            assertCapacity(target.uuid, dst.totalMemory - vm.memorySize.intdiv(2), dst.cpuNum * 4 - vm.cpuNum,
                    "landed VM retains target memory ratio and full vCPU usage")
        } finally {
            hosts.each { host ->
                if (oldMemory[host.uuid] == null) {
                    memoryRatios.deleteMemoryRatio(host.uuid)
                } else {
                    memoryRatios.setMemoryRatio(host.uuid, oldMemory[host.uuid])
                }
                if (oldCpu[host.uuid] == null) {
                    cpuRatios.deleteRatio(host.uuid)
                } else {
                    cpuRatios.setRatio(host.uuid, oldCpu[host.uuid])
                }
            }
            recalculate("clusterUuid", source.clusterUuid)
        }
    }

    void testConcurrentMigrations() {
        def reset = migrate(vm.uuid, source.uuid)
        assert reset.error == null : "Reset VM source: expected=success actual=${reset.error}"
        VmInstanceInventory second = createVmInstance {
            name = "second-vm"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            hostUuid = source.uuid
        }
        HostCapacityVO before = dbFindByUuid(target.uuid, HostCapacityVO.class)
        long memory = before.availableMemory - vm.memorySize - second.memorySize
        long cpu = before.availableCpu - vm.cpuNum - second.cpuNum
        CountDownLatch entered = new CountDownLatch(2)
        Map<String, CountDownLatch> releases = [(vm.uuid): new CountDownLatch(1), (second.uuid): new CountDownLatch(1)]
        env.afterSimulator(KVM_MIGRATE_VM_PATH) { rsp, entity ->
            String uuid = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.MigrateVmCmd.class).vmUuid
            entered.countDown()
            boolean released = releases[uuid].await(120, TimeUnit.SECONDS)
            assert released : "Migration gate: expected=released actual=${released}"
            return rsp
        }
        def executor = Executors.newFixedThreadPool(2)
        try {
            def first = executor.submit({ -> migrate(vm.uuid, target.uuid) } as Callable)
            def next = executor.submit({ -> migrate(second.uuid, target.uuid) } as Callable)
            boolean both = entered.await(30, TimeUnit.SECONDS)
            assert both : "Two migrations must reach agents: expected=true actual=${both}"
            recalculate("hostUuid", target.uuid)
            assertCapacity(target.uuid, memory, cpu, "two target reservations must accumulate")
            releases[vm.uuid].countDown()
            def firstResult = first.get(30, TimeUnit.SECONDS)
            assert firstResult.error == null : "First migration: expected=success actual=${firstResult.error}"
            recalculate("hostUuid", target.uuid)
            assertCapacity(target.uuid, memory, cpu, "landed first VM plus pending second VM")
            releases[second.uuid].countDown()
            def nextResult = next.get(30, TimeUnit.SECONDS)
            assert nextResult.error == null : "Second migration: expected=success actual=${nextResult.error}"
            assert !hasReservation(second.uuid) : "Second completed migration must release target capacity"
        } finally {
            releases.values().each { it.countDown() }
            executor.shutdown()
            boolean stopped = executor.awaitTermination(60, TimeUnit.SECONDS)
            assert stopped : "Migration workers: expected=terminated actual=${stopped}"
            env.afterSimulator(KVM_MIGRATE_VM_PATH) { rsp -> rsp }
        }
    }

    void testRemoteEventsAndMemoryRatio() {
        HostCapacityVO landed = dbFindByUuid(target.uuid, HostCapacityVO.class)
        String taskId = Platform.getUuid()
        deliverRemote(target.uuid, true, taskId)
        recalculate("hostUuid", target.uuid)
        assertCapacity(target.uuid, landed.availableMemory - vm.memorySize, landed.availableCpu - vm.cpuNum,
                "keep an extra reservation until the migration release event arrives")
        recalculate("hostUuid", target.uuid)
        assertCapacity(target.uuid, landed.availableMemory - vm.memorySize, landed.availableCpu - vm.cpuNum,
                "repeated recalculation must not accumulate the extra reservation")
        deliverRemote(target.uuid, false, taskId)
        recalculate("hostUuid", target.uuid)
        assertCapacity(target.uuid, landed.availableMemory, landed.availableCpu, "release restores settled capacity")
        HostCapacityOverProvisioningManager ratios = bean(HostCapacityOverProvisioningManager.class)
        Double oldOverride = ratios.allMemoryRatio[source.uuid]
        HostCapacityVO empty = dbFindByUuid(source.uuid, HostCapacityVO.class)
        try {
            ratios.setMemoryRatio(source.uuid, 2)
            taskId = Platform.getUuid()
            deliverRemote(source.uuid, true, taskId)
            def reserved = cache()[vm.uuid + ":" + taskId]
            deliverRemote(source.uuid, true, taskId)
            assert cache()[vm.uuid + ":" + taskId].is(reserved) : "Duplicate reserve must not extend expiration"
            recalculate("hostUuid", source.uuid)
            assertCapacity(source.uuid, empty.totalMemory - vm.memorySize.intdiv(2), empty.totalCpu - vm.cpuNum,
                    "remote pending memory must use target host ratio")
            deliverRemote(source.uuid, false, taskId)
            assert !cache().containsKey(vm.uuid + ":" + taskId) : "Release must remove the entry"
            deliverRemote(source.uuid, false, taskId)
            assert !cache().containsKey(vm.uuid + ":" + taskId) : "Duplicate release must leave no terminal record"
            deliverRemote(source.uuid, true, taskId)
            assert hasReservation(vm.uuid) : "A rerun of the same task must be able to reserve again"
            deliverRemote(source.uuid, false, taskId)
            String nextTask = Platform.getUuid()
            deliverRemote(source.uuid, true, nextTask)
            deliverRemote(source.uuid, false, taskId)
            assert cache().containsKey(vm.uuid + ":" + nextTask) : "Old task release must preserve a new task"
            String otherVm = Platform.getUuid()
            deliverRemote(source.uuid, true, nextTask, otherVm)
            deliverRemote(source.uuid, false, nextTask)
            assert cache().containsKey(otherVm + ":" + nextTask) : "Batch task must keep separate VM reservations"
            deliverRemote(source.uuid, false, nextTask, otherVm)
            recalculate("hostUuid", source.uuid)
            assertCapacity(source.uuid, empty.totalMemory, empty.totalCpu, "remote release clears pending allocation")
        } finally {
            deliverRemote(source.uuid, false, taskId)
            if (oldOverride == null) {
                ratios.deleteMemoryRatio(source.uuid)
            } else {
                ratios.setMemoryRatio(source.uuid, oldOverride)
            }
            recalculate("hostUuid", source.uuid)
        }
    }

    private void deliverRemote(String hostUuid, boolean add, String taskId, String vmUuid = vm.uuid) {
        CountDownLatch evaluated = new CountDownLatch(1)
        MigrationHostCapacityData data = new MigrationHostCapacityData() {
            @Override
            String getTaskId() {
                evaluated.countDown()
                return super.getTaskId()
            }
        }
        data.vmUuid = vmUuid
        data.hostUuid = hostUuid
        data.memorySize = vm.memorySize
        data.cpuNum = vm.cpuNum
        data.taskId = taskId
        CanonicalEvent event = new CanonicalEvent()
        event.path = add ? VmCanonicalEvents.VM_MIGRATION_HOST_CAPACITY_PATH :
                VmCanonicalEvents.VM_MIGRATION_HOST_CAPACITY_RELEASED_PATH
        event.managementNodeId = "simulated-remote-mn"
        event.content = data
        events.handleEvent(event)
        boolean processed = evaluated.await(10, TimeUnit.SECONDS)
        assert processed : "Remote event must reach cache update: expected=true actual=${processed}"
        // Wait for the receiving callback to finish, including rejected events.
        synchronized (allocator) {
        }
    }

    void testInternalTaskContext() {
        Map<String, String> original = ThreadContext.getContext()
        String taskId = Platform.getUuid()
        def inventory = org.zstack.header.vm.VmInstanceInventory.valueOf(dbFindByUuid(vm.uuid, VmInstanceVO.class))
        try {
            ThreadContext.clearMap()
            ThreadContext.put(Constants.THREAD_CONTEXT_TASK, taskId)
            allocator.beforeMigrateVm(inventory, source.uuid)
            assert cache().containsKey(vm.uuid + ":" + taskId) : "Internal migration must reuse its task ID"
            allocator.failedToMigrateVm(inventory, source.uuid, null)
            assert !cache().containsKey(vm.uuid + ":" + taskId) : "Failure must remove the internal task reservation"
        } finally {
            ThreadContext.clearMap()
            ThreadContext.putAll(original)
        }
    }

    void testExpiredMigrationCapacity() {
        AtomicLong now = new AtomicLong(System.nanoTime())
        Ticker ticker = new Ticker() {
            @Override
            long read() {
                return now.get()
            }
        }
        def actual = allocator.@migrationHostCapacities
        def field = cache().getClass().getDeclaredField("ticker")
        field.accessible = true
        def originalTicker = field.get(cache())
        try {
            field.set(cache(), ticker)
            HostCapacityVO before = dbFindByUuid(source.uuid, HostCapacityVO.class)
            String expired = Platform.getUuid()
            String active = Platform.getUuid()
            deliverRemote(source.uuid, true, expired)
            now.addAndGet(TimeUnit.DAYS.toNanos(2))
            assert cache().containsKey(vm.uuid + ":" + expired) : "Long migration reserve must outlive one hour"
            deliverRemote(source.uuid, true, active)
            now.addAndGet(TimeUnit.HOURS.toNanos(23))
            deliverRemote(source.uuid, true, expired)
            now.addAndGet(TimeUnit.HOURS.toNanos(2))
            assert !cache().containsKey(vm.uuid + ":" + expired) : "Duplicate reserve must not extend the 3-day limit"
            recalculate("hostUuid", source.uuid)
            assertCapacity(source.uuid, before.availableMemory - vm.memorySize, before.availableCpu - vm.cpuNum,
                    "expired reservations must disappear while a newer task remains reserved")
            now.addAndGet(TimeUnit.DAYS.toNanos(2))
            assert !cache().containsKey(vm.uuid + ":" + active) : "Reading a reservation must not extend its lifetime"
            deliverRemote(source.uuid, false, active)
            assert !cache().containsKey(vm.uuid + ":" + active) : "Release after expiry must not create a record"
            recalculate("hostUuid", source.uuid)
            assertCapacity(source.uuid, before.availableMemory, before.availableCpu, "expiry restores host capacity")
        } finally {
            actual.invalidateAll()
            field.set(cache(), originalTicker)
        }
    }

    private boolean hasReservation(String vmUuid) {
        return cache().values().any { it.vmUuid == vmUuid }
    }

    private Map cache() {
        return allocator.@migrationHostCapacities.asMap()
    }

    private MigrateVmAction.Result migrate(String uuid, String hostUuid) {
        MigrateVmAction action = new MigrateVmAction()
        action.apiId = Platform.getUuid()
        migrationTaskIds[uuid] = action.apiId
        action.vmInstanceUuid = uuid
        action.hostUuid = hostUuid
        action.sessionId = adminSession()
        return action.call()
    }

    private void recalculate(String scope, String uuid) {
        RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg()
        msg[scope] = uuid
        allocator.handleMessage(msg)
    }

    private void assertCapacity(String uuid, long memory, long cpu, String reason) {
        HostCapacityVO actual = dbFindByUuid(uuid, HostCapacityVO.class)
        assert actual.availableMemory == memory :
                "${reason}: availableMemory expected=${memory} actual=${actual.availableMemory}"
        assert actual.availableCpu == cpu : "${reason}: availableCpu expected=${cpu} actual=${actual.availableCpu}"
    }

    private void assertNoHistory(String uuid) {
        long count = Q.New(VmSchedHistoryVO.class).eq(VmSchedHistoryVO_.vmInstanceUuid, uuid).count()
        assert count == 0 : "Ordinary migration must not need scheduling history: expected=0 actual=${count}"
    }

    @Override
    void clean() {
        env.delete()
    }
}
