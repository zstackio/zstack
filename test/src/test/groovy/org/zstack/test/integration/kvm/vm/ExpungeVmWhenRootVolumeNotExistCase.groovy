package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.compute.vm.VmSystemTags
import org.zstack.compute.vm.DeleteVmGC
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.errorcode.GlobalErrorCodeI18nService
import org.zstack.core.gc.GCCompletion
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorVO_
import org.zstack.core.jsonlabel.JsonLabel
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.core.Completion
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostInventory as InternalHostInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMVmFirmwareFlashGC
import org.zstack.kvm.KVMVmFirmwareFlashCleanup
import org.zstack.kvm.KVMVmFirmwareFlashCleanupMsg
import org.zstack.core.thread.ThreadFacade
import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.zstack.header.message.MessageReply
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmInstanceInventory as InternalVmInstanceInventory
import org.zstack.header.vm.VmStateChangedExtensionPoint
import org.zstack.header.vm.VmBeforeStartOnHypervisorExtensionPoint
import org.zstack.header.vm.VmInstanceSpec
import org.zstack.header.vm.VmInstanceDeletionMsg
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmJustBeforeDeleteFromDbExtensionPoint
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.DeleteTagAction
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig
import org.zstack.test.integration.identity.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2017/9/15.
 */
class ExpungeVmWhenRootVolumeNotExistCase extends SubCase {
    static class DelayedFirmwareGC extends KVMVmFirmwareFlashGC {
        final CountDownLatch successEntered = new CountDownLatch(1)
        final CountDownLatch successRelease = new CountDownLatch(1)

        @Override
        protected void success() {
            successEntered.countDown()
            assert successRelease.await(90, TimeUnit.SECONDS) : "Owned GC success-write barrier timed out"
            super.success()
        }
    }

    static class ObservedFirmwareGC extends KVMVmFirmwareFlashGC {
        final CountDownLatch failureReceived = new CountDownLatch(1)
        volatile ErrorCode failure

        @Override
        protected void fail(ErrorCode error) {
            super.fail(error)
            failure = error
            failureReceived.countDown()
        }
    }

    EnvSpec env
    VmInstanceInventory vm
    List<String> firmwareVms = []
    List<Map> firmwareRequests = new CopyOnWriteArrayList<Map>()
    Map<String, Object> malformedFirmwareResponses = new ConcurrentHashMap<>()
    volatile String blockedFirmwareUuid
    CountDownLatch firmwareEntered
    CountDownLatch firmwareRelease
    volatile boolean rejectFirmwareCleanup
    volatile boolean legacyFirmwareCleanup

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory

            Closure previous = env.httpHandlers[KVMConstant.CLEAN_FIRMWARE_FLASH]
            env.simulator(KVMConstant.CLEAN_FIRMWARE_FLASH) { HttpEntity<String> e ->
                def command = json(e.body, Map.class)
                boolean rejected = rejectFirmwareCleanup
                boolean legacy = legacyFirmwareCleanup
                Object malformed = malformedFirmwareResponses.get(command.vmUuid)
                firmwareRequests.add([uuid: command.vmUuid,
                                      hostUuid: e.headers.getFirst(org.zstack.header.Constants.AGENT_HTTP_HEADER_RESOURCE_UUID),
                                      vmExisted: dbFindByUuid(command.vmUuid, VmInstanceVO.class) != null,
                                      cleanupInactiveDomain: command.cleanupInactiveDomain, rejected: rejected, legacy: legacy,
                                      malformed: malformed != null])
                if (command.vmUuid == blockedFirmwareUuid) {
                    firmwareEntered.countDown()
                    assert firmwareRelease.await(90, TimeUnit.SECONDS) : "Cleanup barrier timed out"
                }
                if (malformed != null) {
                    return [success: true, cleanupCompleted: malformed]
                }
                Map response = [success: !rejected]
                if (!legacy) {
                    response.cleanupCompleted = !rejected
                }
                if (rejected) {
                    response.error = "firmware file is still in use"
                }
                return response
            }
            Throwable failure = null
            try {
                testDbOnlyRetainsRecreatedFirmwareAgainstPendingGc()
                testMalformedFirmwareCleanupResponseReleasesVmChain()
                testLegacyDeleteExtensionReceivesPolicyOverload()
                Map dbOnly = testFirmwareCleanupPreservesDbOnlyDeletion()
                testFirmwareHostTagCannotBeDeletedByApi()
                assert dbOnly.preserved :
                        "DBOnly deletion must preserve host resources without firmware GC or cleanup request; actual=${dbOnly}"
                testFirmwareCleanupPersistsNewDemandBeforeOldSuccessIsStored()
                testFirmwareCleanupIncludesFailedMigrationTarget()
                testFirmwareCleanupAfterFailedCreate()
                testFirmwareCleanupIncludesFailedStartHost()
                testFirmwareCleanupIncludesPreviousHosts()
                testFirmwareCleanupWaitsForAgentUpgrade()
                testFirmwareCleanupPreservesRecoverableVm()
                testFirmwareCleanupRetriesAfterExpunge()
                testFirmwareCleanupAfterBootModeChangedToLegacy()
                testFirmwareCleanupProtectsRecreatedUuid()
                testFirmwareCleanupAfterDirectDeletion()
                testRootVolumeRecordDeletedExpungeVmStillSuccess()
            } catch (Throwable error) {
                failure = error
                throw error
            } finally {
                rejectFirmwareCleanup = false
                legacyFirmwareCleanup = false
                malformedFirmwareResponses.clear()
                try {
                    cleanupFirmwareVms(failure)
                } finally {
                    if (previous == null) {
                        env.httpHandlers.remove(KVMConstant.CLEAN_FIRMWARE_FLASH)
                    } else {
                        env.httpHandlers[KVMConstant.CLEAN_FIRMWARE_FLASH] = previous
                    }
                }
            }
        }
    }

    void testDbOnlyRetainsRecreatedFirmwareAgainstPendingGc() {
        VmInstanceInventory candidate = createFirmwareVm()
        stopVmInstance { uuid = candidate.uuid }
        destroyVmInstance { uuid = candidate.uuid }
        String identifier = candidate.uuid
        rejectFirmwareCleanup = true
        try {
            expungeVmInstance { uuid = identifier }
            GarbageCollectorVO oldJob = firmwareJob(identifier)
            assert oldJob != null : "The first final deletion must persist a real pending firmware GC"
            assertFirmwareGcRetried(identifier, oldJob.uuid, "rejected")
            GarbageCollectorVO stale = dbFindByUuid(oldJob.uuid, GarbageCollectorVO.class)
            VmInstanceInventory recreated = createFirmwareVm(identifier)
            assert recreated.hostUuid == candidate.hostUuid : "The rebuilt VM must use the same host and firmware path"
            stopVmInstance { uuid = identifier }
            assert dbFindByUuid(identifier, VmInstanceVO.class).state == VmInstanceState.Stopped :
                    "DBOnly retention must be tested on a real stopped, recreated VM"
            int requestsBefore = firmwareRequests.count { it.uuid == identifier && it.cleanupInactiveDomain == true }
            VmInstanceDeletionMsg deletion = new VmInstanceDeletionMsg()
            deletion.vmInstanceUuid = identifier
            deletion.deletionPolicy = "DBOnly"
            CloudBus bus = bean(CloudBus.class)
            bus.makeTargetServiceIdByResourceUuid(deletion, VmInstanceConstant.SERVICE_ID, identifier)
            MessageReply reply = bus.call(deletion)
            assert reply.success && dbFindByUuid(identifier, VmInstanceVO.class) == null :
                    "The recreated VM must complete the real DBOnly owner chain and disappear only from the database"
            rejectFirmwareCleanup = false
            assert dbFindByUuid(oldJob.uuid, GarbageCollectorVO.class).status == GCStatus.Done :
                    "DBOnly must retire the old firmware GC in the same transaction as VM deletion"
            assert new JsonLabel().get("kvm-vm-firmware-generation-" + identifier) == null :
                    "Revoking cleanup must not leave a resource-less generation label"

            KVMVmFirmwareFlashCleanupMsg queued = new KVMVmFirmwareFlashCleanupMsg()
            queued.vmUuid = identifier
            queued.hostUuid = candidate.hostUuid
            queued.jobUuid = oldJob.uuid
            bus.makeTargetServiceIdByResourceUuid(queued, "kvmVmFirmwareFlash", identifier)
            Map replay = new ConcurrentHashMap()
            bus.send(queued, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply response) { replay.reply = response }
            })
            retryInSecs(10) {
                assert replay.reply != null && replay.reply.success : "A queued request for the retired GC must finish harmlessly"
            }
            KVMVmFirmwareFlashGC reloaded = new KVMVmFirmwareFlashGC()
            reloaded.load(stale)
            reloaded.fail(new ErrorCode("SYS.1006", "owned delayed firmware cleanup failure"))
            assert dbFindByUuid(oldJob.uuid, GarbageCollectorVO.class).status == GCStatus.Done :
                    "A stale HA load and late failure must leave the original job Done"
            deleteGCJob { uuid = oldJob.uuid }
            reloaded.load(stale)
            reloaded.fail(new ErrorCode("SYS.1006", "owned delayed failure for a purged firmware cleanup"))
            assert dbFindByUuid(oldJob.uuid, GarbageCollectorVO.class) == null :
                    "A stale HA load and late failure must not recreate the purged job"
            replay.clear()
            KVMVmFirmwareFlashCleanupMsg purged = new KVMVmFirmwareFlashCleanupMsg()
            purged.vmUuid = identifier
            purged.hostUuid = candidate.hostUuid
            purged.jobUuid = oldJob.uuid
            bus.makeTargetServiceIdByResourceUuid(purged, "kvmVmFirmwareFlash", identifier)
            bus.send(purged, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply response) { replay.reply = response }
            })
            retryInSecs(10) {
                assert replay.reply != null && replay.reply.success : "A queued request for a purged GC must finish harmlessly"
            }
            assert firmwareRequests.count { it.uuid == identifier && it.cleanupInactiveDomain == true } == requestsBefore :
                    "Old messages, HA reload and late failure must not reach the Agent after DBOnly"

            Set<String> oldIds = firmwareJobs(identifier)*.uuid as Set<String>
            VmInstanceInventory finalVm = createFirmwareVm(identifier)
            stopVmInstance { uuid = identifier }
            destroyVmInstance { uuid = identifier }
            expungeVmInstance { uuid = identifier }
            List<GarbageCollectorVO> newJobs = firmwareJobs(identifier).findAll { !oldIds.contains(it.uuid) }
            assert !newJobs.empty : "A later real final deletion must persist a new cleanup intent"
            newJobs.each { triggerFirmwareGc(it.uuid) }
            retryInSecs(15) {
                assert newJobs.every { dbFindByUuid(it.uuid, GarbageCollectorVO.class).status == GCStatus.Done } :
                        "The new final-deletion intent must remain allowed after DBOnly invalidates old cleanup"
            }
            assert firmwareRequests.findAll { it.uuid == identifier && it.cleanupInactiveDomain == true }
                    .drop(requestsBefore).any { it.hostUuid == finalVm.hostUuid && !it.vmExisted } :
                    "The new final deletion must receive its own authorized Agent cleanup callback"
        } finally {
            rejectFirmwareCleanup = false
        }
    }

    void testMalformedFirmwareCleanupResponseReleasesVmChain() {
        [[invalid: "boolean"], [true]].each { Object malformed ->
            VmInstanceInventory candidate = createFirmwareVm()
            stopVmInstance { uuid = candidate.uuid }
            destroyVmInstance { uuid = candidate.uuid }
            expungeVmInstance { uuid = candidate.uuid }
            GarbageCollectorVO original = firmwareJob(candidate.uuid)
            assert original != null : "Final deletion must persist the GC used for malformed-response coverage"
            deleteGCJob { uuid = original.uuid }
            ObservedFirmwareGC gc = new ObservedFirmwareGC()
            gc.NAME = original.name
            gc.vmUuid = candidate.uuid
            gc.hostUuid = candidate.hostUuid ?: candidate.lastHostUuid
            gc.submit(60L, TimeUnit.SECONDS)
            malformedFirmwareResponses[candidate.uuid] = malformed
            try {
                triggerGCJob { uuid = gc.uuid }
                retryInSecs(10) {
                    assert firmwareRequests.any { it.uuid == candidate.uuid && it.malformed } :
                            "The real Agent boundary must return a malformed boolean for the owned GC"
                }
                assert gc.failureReceived.await(10, TimeUnit.SECONDS) :
                        "Malformed cleanupCompleted must deliver an error reply to the real GC instead of stalling its VM chain"
                assert gc.failure != null && dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Idle :
                        "The malformed response must complete failure handling and leave the persisted GC retryable"
                CountDownLatch chainReleased = new CountDownLatch(1)
                bean(ThreadFacade.class).chainSubmit(new ChainTask(null) {
                    @Override
                    String getSyncSignature() { return VmInstanceConstant.VM_SYNC_SIGNATURE_PREFIX + candidate.uuid }
                    @Override
                    String getName() { return "probe-malformed-firmware-response-" + candidate.uuid }
                    @Override
                    void run(SyncTaskChain chain) {
                        chainReleased.countDown()
                        chain.next()
                    }
                })
                assert chainReleased.await(10, TimeUnit.SECONDS) :
                        "A task queued on the same VM owner chain must run after the malformed response"
                malformedFirmwareResponses.remove(candidate.uuid)
                triggerFirmwareGc(gc.uuid)
                retryInSecs(45) {
                    assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done :
                            "The same persisted GC must finish after the Agent response is corrected"
                }
            } finally {
                malformedFirmwareResponses.remove(candidate.uuid)
                if (dbFindByUuid(gc.uuid, GarbageCollectorVO.class) != null) {
                    deleteGCJob { uuid = gc.uuid }
                }
            }
        }
    }

    void testLegacyDeleteExtensionReceivesPolicyOverload() {
        List<String> calls = []
        VmJustBeforeDeleteFromDbExtensionPoint legacy = new VmJustBeforeDeleteFromDbExtensionPoint() {
            @Override
            void vmJustBeforeDeleteFromDb(org.zstack.header.vm.VmInstanceInventory inventory) {
                calls.add(inventory.uuid)
            }
        }
        def inventory = InternalVmInstanceInventory.valueOf(dbFindByUuid(vm.uuid, VmInstanceVO.class))
        legacy.vmJustBeforeDeleteFromDb(inventory, VmInstanceDeletionPolicy.DBOnly)
        legacy.vmJustBeforeDeleteFromDb(inventory, VmInstanceDeletionPolicy.Direct)
        assert calls == [vm.uuid, vm.uuid] : "The new policy overload must preserve existing single-argument extension implementations"
    }

    void testFirmwareCleanupPersistsNewDemandBeforeOldSuccessIsStored() {
        VmInstanceInventory candidate = createFirmwareVm()
        stopVmInstance { uuid = candidate.uuid }
        destroyVmInstance { uuid = candidate.uuid }
        expungeVmInstance { uuid = candidate.uuid }
        List<GarbageCollectorVO> firstJobs = firmwareJobs(candidate.uuid)
        assert firstJobs.size() == 1 : "First final deletion must persist one source-host GC"
        GarbageCollectorVO original = firstJobs[0]
        deleteGCJob { uuid = original.uuid }
        assert firmwareJobs(candidate.uuid).empty : "Replace only the original test timer with a delayed-success fixture"
        DelayedFirmwareGC delayed = new DelayedFirmwareGC()
        delayed.NAME = original.name
        delayed.vmUuid = candidate.uuid
        delayed.hostUuid = candidate.hostUuid
        try {
            delayed.submit(60L, TimeUnit.SECONDS)
            String delayedUuid = delayed.uuid
            triggerGCJob { uuid = delayedUuid }
            assert delayed.successEntered.await(45, TimeUnit.SECONDS) :
                    "The delayed GC must complete the real Agent cleanup before holding its Done write"
            assert firmwareRequests.any { it.uuid == candidate.uuid && !it.rejected && !it.legacy } :
                    "The pending-success fixture must have actually received a valid cleanup acknowledgement"
            retryInSecs {
                def chain = bean(ThreadFacade.class).getChainTaskInfo("Vm-" + candidate.uuid)
                assert chain == null || chain.runningTask.empty :
                        "The VM owner chain must be released before the old GC stores Done"
            }
            assert dbFindByUuid(delayedUuid, GarbageCollectorVO.class).status != GCStatus.Done :
                    "The first real cleanup must remain pending while its success callback is held"
            VmInstanceInventory recreated = createFirmwareVm(candidate.uuid)
            assert recreated.state == VmInstanceState.Running.toString() : "Second real API create must reach Running"
            stopVmInstance { uuid = recreated.uuid }
            destroyVmInstance { uuid = recreated.uuid }
            int requestsBeforeSecondDeletion = firmwareRequests.count { it.uuid == candidate.uuid }
            expungeVmInstance { uuid = recreated.uuid }
            assert dbFindByUuid(candidate.uuid, VmInstanceVO.class) == null : "Second final deletion must remove its VM row"
            assert dbFindByUuid(delayedUuid, GarbageCollectorVO.class).status != GCStatus.Done :
                    "The old GC must still be pending when the second deletion registers its cleanup"
            List<GarbageCollectorVO> secondJobs = firmwareJobs(candidate.uuid)
            Map observed = [vmUuid: candidate.uuid, oldJobUuid: delayedUuid, oldName: original.name,
                            newProductionJobUuids: secondJobs*.uuid]
            delayed.successRelease.countDown()
            retryInSecs {
                assert dbFindByUuid(delayedUuid, GarbageCollectorVO.class).status == GCStatus.Done :
                        "The old completed cleanup must store Done after release"
            }
            assert !secondJobs.empty :
                    "A real second deletion must persist new cleanup after old Agent success but before old Done; actual=${observed}"
            assert secondJobs.every { it.name == original.name && it.uuid != delayedUuid } :
                    "Independent deletion tasks must keep the VM/host name and use distinct durable IDs"
            secondJobs.each { GarbageCollectorVO job -> triggerFirmwareGc(job.uuid) }
            retryInSecs {
                assert secondJobs.every { dbFindByUuid(it.uuid, GarbageCollectorVO.class).status == GCStatus.Done } :
                        "Each new production GC must complete independently of the old Done row"
                List<Map> later = firmwareRequests.findAll { it.uuid == candidate.uuid }.drop(requestsBeforeSecondDeletion)
                assert !later.empty && later.any {
                    it.hostUuid == recreated.hostUuid && it.cleanupInactiveDomain == true && !it.vmExisted
                } : "The second deletion must receive its own final cleanup callback"
            }
        } finally {
            delayed.successRelease.countDown()
            if (delayed.uuid != null && dbFindByUuid(delayed.uuid, GarbageCollectorVO.class) != null) {
                deleteGCJob { uuid = delayed.uuid }
            }
        }
    }

    Map testFirmwareCleanupPreservesDbOnlyDeletion() {
        VmInstanceInventory candidate = createFirmwareVm()
        stopVmInstance { uuid = candidate.uuid }
        assert dbFindByUuid(candidate.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped :
                "DBOnly regression requires a stopped VM"
        assert VmSystemTags.BOOT_MODE.getTokenByResourceUuid(candidate.uuid, VmSystemTags.BOOT_MODE_TOKEN) == "UEFI" :
                "DBOnly regression requires existing UEFI firmware"
        int requestsBefore = firmwareRequests.count { it.uuid == candidate.uuid }
        VmInstanceDeletionMsg deletion = new VmInstanceDeletionMsg()
        deletion.vmInstanceUuid = candidate.uuid
        deletion.deletionPolicy = "DBOnly"
        CloudBus bus = bean(CloudBus.class)
        bus.makeTargetServiceIdByResourceUuid(deletion, VmInstanceConstant.SERVICE_ID, candidate.uuid)
        MessageReply reply = bus.call(deletion)
        assert reply.success : "The real VM owner DBOnly deletion chain must succeed: ${reply.error}"
        assert dbFindByUuid(candidate.uuid, VmInstanceVO.class) == null :
                "DBOnly must remove the owned VM database row"
        List<String> jobs = firmwareJobs(candidate.uuid)*.uuid
        int addedRequests = firmwareRequests.count { it.uuid == candidate.uuid } - requestsBefore
        return [preserved: jobs.empty && addedRequests == 0, vmUuid: candidate.uuid,
                gcUuids: jobs, firmwareRequestsAdded: addedRequests]
    }

    void testFirmwareHostTagCannotBeDeletedByApi() {
        VmInstanceInventory candidate = createFirmwareVm()
        SystemTagVO tag = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, candidate.uuid)
                .eq(SystemTagVO_.inherent, true)
                .eq(SystemTagVO_.tag, "kvm::firmware::host::${candidate.hostUuid}".toString()).find()
        assert tag != null : "Internal host recording must create an inherent firmware host tag"
        def result = new DeleteTagAction(uuid: tag.uuid, sessionId: env.session.uuid).call()
        assert result.error != null && dbFindByUuid(tag.uuid, SystemTagVO.class)?.inherent :
                "Existing API protection must reject inherent firmware tag deletion even for the administrator"
        stopVmInstance { uuid = candidate.uuid }
        destroyVmInstance { uuid = candidate.uuid }
        expungeVmInstance { uuid = candidate.uuid }
        assert dbFindByUuid(tag.uuid, SystemTagVO.class) == null :
                "Normal internal final deletion must still remove its inherent tags"
    }

    void testFirmwareCleanupIncludesFailedMigrationTarget() {
        VmInstanceInventory candidate = createFirmwareVm()
        String sourceHostUuid = candidate.hostUuid
        HostInventory target = addKVMHost {
            name = "firmware-failed-migration-target"
            managementIp = "127.0.0.31"
            username = "root"
            password = "password"
            clusterUuid = candidate.clusterUuid
        } as HostInventory
        def config = LocalStoragePrimaryStorageGlobalConfig.ALLOW_LIVE_MIGRATION
        String oldMigrationSetting = config.value()
        String path = KVMConstant.KVM_MIGRATE_VM_PATH
        Closure previous = env.getPostSimulator(path)
        Map observed = new ConcurrentHashMap()
        try {
            updateGlobalConfig { category = config.category; name = config.name; value = "true" }
            env.afterSimulator(path) { response, HttpEntity<String> request ->
                if (previous != null) {
                    if (previous.maximumNumberOfParameters <= 1) { response = previous(response) }
                    else if (previous.maximumNumberOfParameters == 2) { response = previous(response, request) }
                    else { response = previous(response, request, env) }
                }
                KVMAgentCommands.MigrateVmCmd command = json(request.body, KVMAgentCommands.MigrateVmCmd.class)
                if (command.vmUuid == candidate.uuid) {
                    observed.agentReached = true
                    observed.destinationIp = command.destHostManagementIp ?: command.destHostIp
                    VmInstanceVO duringMigration = dbFindByUuid(candidate.uuid, VmInstanceVO.class)
                    observed.currentHost = duringMigration.hostUuid
                    observed.lastHost = duringMigration.lastHostUuid ?: ""
                    response.success = false
                    response.error = "owned live migration reached the agent but failed on its target"
                }
                return response
            }
            expect(AssertionError.class) {
                migrateVm { vmInstanceUuid = candidate.uuid; hostUuid = target.uuid }
            }
            assert observed.agentReached && observed.destinationIp == target.managementIp :
                    "Failed migration must reach KVM with target B; expectedIp=${target.managementIp}, observed=${observed}"
            assert observed.currentHost == sourceHostUuid && observed.lastHost != target.uuid :
                    "Migration target B must not be current/last host when the agent fails; sourceA=${sourceHostUuid}, targetB=${target.uuid}, observed=${observed}"
            VmInstanceVO rolledBack = dbFindByUuid(candidate.uuid, VmInstanceVO.class)
            assert rolledBack.state == VmInstanceState.Running && rolledBack.hostUuid == sourceHostUuid &&
                    rolledBack.lastHostUuid != target.uuid :
                    "Failed migration must roll back on source A without recording B in current/last; sourceA=${sourceHostUuid}, targetB=${target.uuid}, current=${rolledBack.hostUuid}, last=${rolledBack.lastHostUuid}, state=${rolledBack.state}"
            boolean targetPersisted = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, candidate.uuid)
                    .eq(SystemTagVO_.inherent, true)
                    .eq(SystemTagVO_.tag, "kvm::firmware::host::${target.uuid}".toString()).isExists()
            assert firmwareRequests.any { it.uuid == candidate.uuid && it.hostUuid == target.uuid &&
                    it.cleanupInactiveDomain != true && it.vmExisted } :
                    "Existing pre-migration cleanup must reach target B in default mode while the VM exists"
            assert firmwareJob(candidate.uuid) == null && !firmwareRequests.any {
                it.uuid == candidate.uuid && it.cleanupInactiveDomain == true
            } : "Failed migration must preserve the recoverable VM without final-mode cleanup or a firmware GC"
            stopVmInstance { uuid = candidate.uuid }
            destroyVmInstance { uuid = candidate.uuid }
            expungeVmInstance { uuid = candidate.uuid }
            assert dbFindByUuid(candidate.uuid, VmInstanceVO.class) == null :
                    "Final deletion must remove the failed-migration VM before firmware cleanup"
            List<GarbageCollectorVO> jobs = Q.New(GarbageCollectorVO.class)
                    .eq(GarbageCollectorVO_.runnerClass, KVMVmFirmwareFlashGC.class.name)
                    .like(GarbageCollectorVO_.name, "gc-vm-firmware-${candidate.uuid}-on-host-%".toString()).list()
            String prefix = "gc-vm-firmware-${candidate.uuid}-on-host-"
            Set<String> actualHosts = jobs.collect { it.name.substring(prefix.length()) }.toSet()
            Set<String> expectedHosts = [sourceHostUuid, target.uuid].toSet()
            assert targetPersisted && actualHosts == expectedHosts :
                    "Failed migration target B must be persisted for final firmware GC despite never becoming current/last; sourceA=${sourceHostUuid}, targetB=${target.uuid}, persisted=${targetPersisted}, actualHosts=${actualHosts}"
            jobs.each { GarbageCollectorVO job -> triggerFirmwareGc(job.uuid) }
            retryInSecs {
                assert jobs.every { dbFindByUuid(it.uuid, GarbageCollectorVO.class).status == GCStatus.Done } :
                        "Firmware GC must complete on source A and failed migration target B"
                assert firmwareRequests.any { it.uuid == candidate.uuid && it.hostUuid == target.uuid &&
                        it.cleanupInactiveDomain == true && !it.vmExisted } :
                        "Final cleanup must actually reach the registered failed migration target B after VM deletion"
            }
        } finally {
            if (previous == null) { env.httpPostHandlers.remove(path) } else { env.afterSimulator(path, previous) }
            try {
                updateGlobalConfig { category = config.category; name = config.name; value = oldMigrationSetting }
            } finally {
                deleteHost { uuid = target.uuid }
            }
        }
    }

    private VmInstanceInventory createFirmwareVm(String reusedUuid = null) {
        VmInstanceInventory template = vm
        VmInstanceInventory created = createVmInstance {
            name = "firmware-cleanup"
            resourceUuid = reusedUuid
            instanceOfferingUuid = template.instanceOfferingUuid
            imageUuid = template.imageUuid
            l3NetworkUuids = template.vmNics.collect { it.l3NetworkUuid }
            systemTags = ["bootMode::UEFI"]
        } as VmInstanceInventory
        if (!firmwareVms.contains(created.uuid)) {
            firmwareVms.add(created.uuid)
        }
        return created
    }

    private List<GarbageCollectorVO> firmwareJobs(String vmUuid) {
        return Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.runnerClass, KVMVmFirmwareFlashGC.class.name)
                .like(GarbageCollectorVO_.name, "gc-vm-firmware-${vmUuid}-on-host-%".toString())
                .list()
    }

    private GarbageCollectorVO firmwareJob(String vmUuid) {
        List<GarbageCollectorVO> jobs = firmwareJobs(vmUuid)
        return jobs.find { it.status != GCStatus.Done } ?: jobs.find()
    }

    private void assertFirmwareGcRetried(String vmUuid, String jobUuid, String responseFlag) {
        retryInSecs(90) {
            assert firmwareJobs(vmUuid)*.uuid == [jobUuid] : "Retry must use the same sole persisted firmware GC: ${jobUuid}"
            assert firmwareRequests.count { it.uuid == vmUuid && it.cleanupInactiveDomain == true && it[responseFlag] } >= 2 :
                    "The sole GC must automatically retry after processing its first ${responseFlag} response"
            assert dbFindByUuid(jobUuid, GarbageCollectorVO.class).status == GCStatus.Idle :
                    "The processed ${responseFlag} response must leave the same GC retryable"
        }
    }

    private void triggerFirmwareGc(String jobUuid) {
        try {
            triggerGCJob { uuid = jobUuid }
        } catch (AssertionError error) {
            if (dbFindByUuid(jobUuid, GarbageCollectorVO.class)?.status != GCStatus.Done) {
                throw error
            }
        }
    }

    private void assertFirmwareRetained(VmInstanceInventory candidate, boolean oldJobExists = false) {
        if (!oldJobExists) {
            assert firmwareJob(candidate.uuid) == null : "Recoverable VM must not have a firmware cleanup job: ${candidate.uuid}"
        }
        int requestsBefore = firmwareRequests.count { it.uuid == candidate.uuid }
        def gc = new KVMVmFirmwareFlashGC()
        gc.vmUuid = candidate.uuid
        gc.hostUuid = candidate.hostUuid ?: candidate.lastHostUuid
        Map result = new ConcurrentHashMap()
        gc.triggerNow(new GCCompletion(null) {
            @Override
            void success() { result.success = true }
            @Override
            void cancel() { result.cancel = true }
            @Override
            void fail(ErrorCode error) { result.error = error }
        })
        retryInSecs {
            assert result.error != null && !result.success && !result.cancel : "Existing VM firmware must be retained; callback=${result}"
        }
        String localized = bean(GlobalErrorCodeI18nService.class).getLocalizedMessage(
                result.error.globalErrorCode, "zh_CN", result.error.formatArgs)
        assert localized != null && localized.contains(candidate.uuid) && localized.contains("\u64cd\u4f5c\u5931\u8d25") && !localized.contains("%s") : "Firmware protection message must format all localized arguments: ${localized}"
        assert firmwareRequests.count { it.uuid == candidate.uuid } == requestsBefore : "Cleanup must not reach the agent while VM ${candidate.uuid} still exists"
    }

    void testFirmwareCleanupPreservesRecoverableVm() {
        VmInstanceInventory candidate = createFirmwareVm()
        assertFirmwareRetained(candidate)
        stopVmInstance { uuid = candidate.uuid }
        assertFirmwareRetained(candidate)
        destroyVmInstance { uuid = candidate.uuid }
        assert dbFindByUuid(candidate.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed : "Delay deletion must remain recoverable"
        assertFirmwareRetained(candidate)
        recoverVmInstance { uuid = candidate.uuid }
        assertFirmwareRetained(candidate)
        startVmInstance { uuid = candidate.uuid }
        assert dbFindByUuid(candidate.uuid, VmInstanceVO.class).state == VmInstanceState.Running : "Recovered VM must still start"
    }

    void testFirmwareCleanupWaitsForAgentUpgrade() {
        VmInstanceInventory candidate = createFirmwareVm()
        stopVmInstance { uuid = candidate.uuid }
        destroyVmInstance { uuid = candidate.uuid }
        String jobUuid
        legacyFirmwareCleanup = true
        try {
            expungeVmInstance { uuid = candidate.uuid }
            GarbageCollectorVO job = firmwareJob(candidate.uuid)
            assert job != null : "Expunge must persist the GC used for the rolling-Agent test"
            jobUuid = job.uuid
            assertFirmwareGcRetried(candidate.uuid, jobUuid, "legacy")
        } finally {
            legacyFirmwareCleanup = false
        }
        triggerFirmwareGc(jobUuid)
        retryInSecs {
            assert dbFindByUuid(jobUuid, GarbageCollectorVO.class).status == GCStatus.Done :
                    "The same pending GC must complete when the upgraded Agent acknowledges cleanup"
        }
        assert firmwareJobs(candidate.uuid)*.uuid == [jobUuid] : "Rolling Agent upgrade must reuse one persistent GC UUID"
        assert firmwareRequests.any { it.uuid == candidate.uuid && it.legacy } : "Expected a real old-Agent request"
        assert firmwareRequests.any { it.uuid == candidate.uuid && !it.legacy } : "Expected a real upgraded-Agent retry"
    }

    void testFirmwareCleanupIncludesPreviousHosts() {
        VmInstanceInventory candidate = createFirmwareVm()
        String sourceHostUuid = candidate.hostUuid
        stopVmInstance { uuid = candidate.uuid }
        HostInventory target = addKVMHost {
            name = "firmware-cleanup-secondary"
            managementIp = "127.0.0.29"
            username = "root"
            password = "password"
            clusterUuid = candidate.clusterUuid
        } as HostInventory
        try {
            InternalVmInstanceInventory starting = InternalVmInstanceInventory.valueOf(dbFindByUuid(candidate.uuid, VmInstanceVO.class))
            starting.state = VmInstanceState.Starting.toString()
            bean(PluginRegistry.class).getExtensionList(VmStateChangedExtensionPoint.class)
                    .findAll { it instanceof KVMVmFirmwareFlashCleanup }.each {
                it.vmStateChanged(starting, VmInstanceState.Stopped, VmInstanceState.Starting)
            }
            boolean sourceHostPersisted = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, candidate.uuid)
                    .eq(SystemTagVO_.inherent, true).list().any { it.tag.contains(sourceHostUuid) }

            // Local storage cannot restart on another host; change only this fixture's host history after exercising the registered hook.
            SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, candidate.uuid)
                    .set(VmInstanceVO_.hostUuid, null).set(VmInstanceVO_.lastHostUuid, target.uuid).update()
            destroyVmInstance { uuid = candidate.uuid }
            expungeVmInstance { uuid = candidate.uuid }
            List<GarbageCollectorVO> jobs = Q.New(GarbageCollectorVO.class)
                    .eq(GarbageCollectorVO_.runnerClass, KVMVmFirmwareFlashGC.class.name)
                    .like(GarbageCollectorVO_.name, "gc-vm-firmware-${candidate.uuid}-on-host-%".toString()).list()
            String prefix = "gc-vm-firmware-${candidate.uuid}-on-host-"
            Set<String> actualHosts = jobs.collect { it.name.substring(prefix.length()) }.toSet()
            Set<String> expectedHosts = [sourceHostUuid, target.uuid].toSet()
            assert sourceHostPersisted && actualHosts == expectedHosts :
                    "Final deletion must persist the previous firmware host and clean both hosts; persisted=${sourceHostPersisted}, expectedHosts=${expectedHosts}, actualHosts=${actualHosts}"
            jobs.each { GarbageCollectorVO job ->
                triggerFirmwareGc(job.uuid)
            }
            retryInSecs {
                assert jobs.every { dbFindByUuid(it.uuid, GarbageCollectorVO.class).status == GCStatus.Done } :
                        "Firmware GC must complete on both registered hosts"
            }
        } finally {
            deleteHost { uuid = target.uuid }
        }
    }

    void testFirmwareCleanupAfterFailedCreate() {
        String identifier = Platform.uuid
        firmwareVms.add(identifier)
        String path = KVMConstant.KVM_START_VM_PATH
        Closure previous = env.getPostSimulator(path)
        String destroyPath = KVMConstant.KVM_DESTROY_VM_PATH
        Closure previousDestroy = env.getPostSimulator(destroyPath)
        Map observed = new ConcurrentHashMap()
        String jobUuid
        rejectFirmwareCleanup = true
        env.afterSimulator(path) { response, HttpEntity<String> request ->
            if (previous != null) {
                if (previous.maximumNumberOfParameters <= 1) { response = previous(response) }
                else if (previous.maximumNumberOfParameters == 2) { response = previous(response, request) }
                else { response = previous(response, request, env) }
            }
            if (json(request.body, Map.class).vmInstanceUuid == identifier) {
                String hostUuid = request.headers.getFirst(org.zstack.header.Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
                observed.agentReached = true
                observed.hostUuid = hostUuid
                observed.hostRecorded = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, identifier)
                        .eq(SystemTagVO_.inherent, true).list().any { it.tag.contains(hostUuid) }
                response.success = false
                response.error = "owned create failed after reaching KVM agent"
            }
            return response
        }
        env.afterSimulator(destroyPath) { response, HttpEntity<String> request ->
            if (previousDestroy != null) {
                if (previousDestroy.maximumNumberOfParameters <= 1) { response = previousDestroy(response) }
                else if (previousDestroy.maximumNumberOfParameters == 2) { response = previousDestroy(response, request) }
                else { response = previousDestroy(response, request, env) }
            }
            if (json(request.body, KVMAgentCommands.DestroyVmCmd.class).uuid == identifier) {
                observed.destroyReached = true
                response.success = false
                response.error = "owned create rollback destroy failed"
            }
            return response
        }
        try {
            expect(AssertionError.class) { createFirmwareVm(identifier) }
            assert observed.agentReached : "Failed Create must reach its KVM Agent; observed=${observed}"
            assert observed.hostRecorded :
                    "Create must record its target firmware host before reaching the KVM Agent; observed=${observed}"
            assert observed.destroyReached : "Failed Create must reach rollback destroy and receive its business failure; observed=${observed}"
            assert dbFindByUuid(identifier, VmInstanceVO.class) == null : "Failed Create must remove its VM database row"
            assert !Q.New(GarbageCollectorVO.class).eq(GarbageCollectorVO_.runnerClass, DeleteVmGC.class.name)
                    .like(GarbageCollectorVO_.name, "gc-vm-${identifier}-on-host-%".toString()).isExists() :
                    "Rollback destroy business failure must not schedule a transport-error DeleteVmGC; vmUuid=${identifier}"
            GarbageCollectorVO job = firmwareJob(identifier)
            assert job != null : "Failed Create reaching KVM must schedule firmware GC before removing the VM; vmUuid=${identifier}, host=${observed.hostUuid}"
            jobUuid = job.uuid
            assertFirmwareGcRetried(identifier, jobUuid, "rejected")
        } finally {
            rejectFirmwareCleanup = false
            if (previous == null) { env.httpPostHandlers.remove(path) } else { env.afterSimulator(path, previous) }
            if (previousDestroy == null) { env.httpPostHandlers.remove(destroyPath) } else { env.afterSimulator(destroyPath, previousDestroy) }
        }
        retryInSecs(45) {
            assert dbFindByUuid(jobUuid, GarbageCollectorVO.class).status == GCStatus.Done : "Failed-Create GC must retry automatically after the Agent recovers"
        }
        assert firmwareJob(identifier).uuid == jobUuid : "Failed-Create cleanup must complete the same persistent GC job"
        assert firmwareRequests.findAll { it.uuid == identifier }.every { !it.vmExisted } : "Failed-Create cleanup must wait until the VM database row is removed"
    }

    void testFirmwareCleanupIncludesFailedStartHost() {
        VmInstanceInventory candidate = createFirmwareVm()
        String sourceHostUuid = candidate.hostUuid
        stopVmInstance { uuid = candidate.uuid }
        HostInventory target = addKVMHost {
            name = "firmware-failed-start-target"
            managementIp = "127.0.0.30"
            username = "root"
            password = "password"
            clusterUuid = candidate.clusterUuid
        } as HostInventory
        try {
            InternalVmInstanceInventory starting = InternalVmInstanceInventory.valueOf(dbFindByUuid(candidate.uuid, VmInstanceVO.class))
            starting.state = VmInstanceState.Starting.toString()
            bean(PluginRegistry.class).getExtensionList(VmStateChangedExtensionPoint.class)
                    .findAll { it instanceof KVMVmFirmwareFlashCleanup }.each {
                it.vmStateChanged(starting, VmInstanceState.Stopped, VmInstanceState.Starting)
            }
            VmInstanceSpec spec = new VmInstanceSpec()
            spec.vmInventory = starting
            spec.vmInventory.hypervisorType = null
            spec.destHost = InternalHostInventory.valueOf(dbFindByUuid(target.uuid, HostVO.class))
            bean(PluginRegistry.class).getExtensionList(VmBeforeStartOnHypervisorExtensionPoint.class)
                    .findAll { it instanceof KVMVmFirmwareFlashCleanup }.each { extension ->
                Map result = new ConcurrentHashMap()
                extension.beforeStartVmOnHypervisor(spec, new Completion(null) {
                    @Override
                    void success() { result.success = true }
                    @Override
                    void fail(ErrorCode error) { result.error = error }
                })
                retryInSecs {
                    assert result.success && !result.error : "Registered pre-start hook must finish its Completion callback: ${result}"
                }
            }
            VmInstanceVO rolledBack = dbFindByUuid(candidate.uuid, VmInstanceVO.class)
            assert rolledBack.hostUuid == null && rolledBack.lastHostUuid == sourceHostUuid :
                    "Failed-start fixture must retain source A in DB; source=${sourceHostUuid}, current=${rolledBack.hostUuid}, last=${rolledBack.lastHostUuid}"
            assert !firmwareRequests.any { it.uuid == candidate.uuid } : "Recording a failed start target must preserve the recoverable VM"
            boolean targetPersisted = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, candidate.uuid)
                    .eq(SystemTagVO_.inherent, true).list().any { it.tag.contains(target.uuid) }
            destroyVmInstance { uuid = candidate.uuid }
            expungeVmInstance { uuid = candidate.uuid }
            List<GarbageCollectorVO> jobs = Q.New(GarbageCollectorVO.class)
                    .eq(GarbageCollectorVO_.runnerClass, KVMVmFirmwareFlashGC.class.name)
                    .like(GarbageCollectorVO_.name, "gc-vm-firmware-${candidate.uuid}-on-host-%".toString()).list()
            String prefix = "gc-vm-firmware-${candidate.uuid}-on-host-"
            Set<String> actualHosts = jobs.collect { it.name.substring(prefix.length()) }.toSet()
            Set<String> expectedHosts = [sourceHostUuid, target.uuid].toSet()
            assert targetPersisted && actualHosts == expectedHosts :
                    "Failed start rollback must retain target B for final firmware GC; sourceA=${sourceHostUuid}, targetB=${target.uuid}, persisted=${targetPersisted}, actualHosts=${actualHosts}"
            jobs.each { GarbageCollectorVO job -> triggerFirmwareGc(job.uuid) }
            retryInSecs {
                assert jobs.every { dbFindByUuid(it.uuid, GarbageCollectorVO.class).status == GCStatus.Done } :
                        "Source and failed-start target firmware GC must both complete"
            }
        } finally {
            deleteHost { uuid = target.uuid }
        }
    }

    void testFirmwareCleanupRetriesAfterExpunge() {
        VmInstanceInventory candidate = createFirmwareVm()
        stopVmInstance { uuid = candidate.uuid }
        destroyVmInstance { uuid = candidate.uuid }
        rejectFirmwareCleanup = true
        try {
            expungeVmInstance { uuid = candidate.uuid }
            assert dbFindByUuid(candidate.uuid, VmInstanceVO.class) == null : "Expunge must remove the VM before firmware cleanup"
            GarbageCollectorVO job = firmwareJob(candidate.uuid)
            assert job != null : "Final deletion must schedule firmware cleanup for ${candidate.uuid}"
            assertFirmwareGcRetried(candidate.uuid, job.uuid, "rejected")
        } finally {
            rejectFirmwareCleanup = false
        }
        retryInSecs(45) {
            assert firmwareJob(candidate.uuid).status == GCStatus.Done : "Firmware cleanup must retry automatically after the failure is removed"
        }
        assert firmwareRequests.findAll { it.uuid == candidate.uuid }.size() >= 2 : "Expected failed cleanup and successful automatic retry"
        assert firmwareRequests.any { it.uuid == candidate.uuid && !it.rejected } : "Automatic retry must issue a request after the rejection is removed"
        assert firmwareRequests.findAll { it.cleanupInactiveDomain == true }.every { !it.vmExisted } :
                "Final-mode cleanup must never run while a VM record exists"
    }

    void testFirmwareCleanupAfterBootModeChangedToLegacy() {
        VmInstanceInventory candidate = createFirmwareVm()
        stopVmInstance { uuid = candidate.uuid }
        setVmBootMode { uuid = candidate.uuid; bootMode = "Legacy" }
        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(candidate.uuid, VmSystemTags.BOOT_MODE_TOKEN)
        assert bootMode == "Legacy" : "Expected UEFI VM to use Legacy before deletion, actual=${bootMode}"
        destroyVmInstance { uuid = candidate.uuid }
        expungeVmInstance { uuid = candidate.uuid }
        assert dbFindByUuid(candidate.uuid, VmInstanceVO.class) == null : "Final deletion must remove the Legacy VM record"

        assert firmwareJob(candidate.uuid) != null : "Final Legacy deletion must persist a firmware GC"
        retryInSecs(45) {
            assert firmwareJob(candidate.uuid).status == GCStatus.Done : "Former UEFI VM firmware must be reclaimed after Legacy deletion"
        }
        List<Map> requests = firmwareRequests.findAll { it.uuid == candidate.uuid }
        assert !requests.empty && requests.every { it.cleanupInactiveDomain == true && !it.vmExisted } :
                "Completed Legacy cleanup must have nonempty final-mode requests after DB removal; actual=${requests}"
    }

    void testFirmwareCleanupProtectsRecreatedUuid() {
        VmInstanceInventory candidate = createFirmwareVm()
        stopVmInstance { uuid = candidate.uuid }
        destroyVmInstance { uuid = candidate.uuid }
        firmwareEntered = new CountDownLatch(1)
        firmwareRelease = new CountDownLatch(1)
        blockedFirmwareUuid = candidate.uuid
        def executor = Executors.newSingleThreadExecutor()
        def creation
        String recreatedHostUuid
        Throwable failure = null
        try {
            expungeVmInstance { uuid = candidate.uuid }
            assert firmwareEntered.await(45, TimeUnit.SECONDS) : "Old cleanup must reach the agent boundary"
            creation = executor.submit({ createFirmwareVm(candidate.uuid) } as Callable<VmInstanceInventory>)
            retryInSecs {
                assert dbFindByUuid(candidate.uuid, VmInstanceVO.class) != null : "Same UUID must enter the real API create path"
                def queue = bean(ThreadFacade.class).getChainTaskInfo("Vm-" + candidate.uuid)
                assert queue != null && !queue.pendingTask.empty : "New VM start must be queued behind in-flight cleanup"
            }
            assert !creation.done : "Same-UUID start must wait for old firmware cleanup"
            assert dbFindByUuid(candidate.uuid, VmInstanceVO.class).state != VmInstanceState.Running : "New VM must not start before cleanup finishes"
            firmwareRelease.countDown()
            VmInstanceInventory recreated = creation.get(45, TimeUnit.SECONDS)
            assert recreated.uuid == candidate.uuid
            recreatedHostUuid = recreated.hostUuid
            assertFirmwareRetained(recreated, true)
        } catch (Throwable error) {
            failure = error
            throw error
        } finally {
            firmwareRelease.countDown()
            blockedFirmwareUuid = null
            try {
                if (creation != null) {
                    creation.get(45, TimeUnit.SECONDS)
                }
            } catch (Throwable error) {
                if (failure == null) {
                    throw error
                }
                failure.addSuppressed(error)
            } finally {
                executor.shutdownNow()
            }
        }
        Set<String> oldJobUuids = firmwareJobs(candidate.uuid)*.uuid as Set<String>
        int requestsBeforeSecondDeletion = firmwareRequests.count { it.uuid == candidate.uuid }
        destroyVmInstance { uuid = candidate.uuid }
        expungeVmInstance { uuid = candidate.uuid }
        retryInSecs(45) {
            List<GarbageCollectorVO> newJobs = firmwareJobs(candidate.uuid).findAll { !oldJobUuids.contains(it.uuid) }
            assert !newJobs.empty && newJobs.every { it.status == GCStatus.Done } :
                    "Second final deletion must finish a new persistent GC, not reuse an old Done row; old=${oldJobUuids}, new=${newJobs*.uuid}"
            List<Map> requests = firmwareRequests.findAll { it.uuid == candidate.uuid }
            assert requests.size() > requestsBeforeSecondDeletion &&
                    requests.drop(requestsBeforeSecondDeletion).any {
                        it.hostUuid == recreatedHostUuid && it.cleanupInactiveDomain == true && !it.vmExisted
                    } : "The recreated VM must produce its own final-mode cleanup callback after second deletion"
        }
    }

    void testFirmwareCleanupAfterDirectDeletion() {
        def config = VmGlobalConfig.VM_DELETION_POLICY
        String oldPolicy = config.value()
        Map<String, String> directVms = [:]
        try {
            updateGlobalConfig { category = config.category; name = config.name; value = "Direct" }
            [false, true].each { boolean stopFirst ->
                VmInstanceInventory candidate = createFirmwareVm()
                directVms[candidate.uuid] = candidate.hostUuid
                if (stopFirst) {
                    stopVmInstance { uuid = candidate.uuid }
                }
                destroyVmInstance { uuid = candidate.uuid }
                assert dbFindByUuid(candidate.uuid, VmInstanceVO.class) == null : "Direct deletion must remove VM ${candidate.uuid}"
                assert firmwareJob(candidate.uuid) != null : "Direct deletion must schedule firmware cleanup, stopped=${stopFirst}"
            }
        } finally {
            updateGlobalConfig { category = config.category; name = config.name; value = oldPolicy }
        }
        retryInSecs(45) {
            directVms.each { String uuid, String hostUuid ->
                assert firmwareJob(uuid).status == GCStatus.Done : "Direct deletion firmware must be cleaned: ${uuid}"
                List<Map> requests = firmwareRequests.findAll {
                    it.uuid == uuid && it.hostUuid == hostUuid && it.cleanupInactiveDomain == true
                }
                assert !requests.empty && requests.every { !it.vmExisted } :
                        "Each Direct deletion must receive its own final callback on its expected host after DB removal; uuid=${uuid}, host=${hostUuid}, requests=${requests}"
            }
        }
        assert firmwareRequests.findAll { it.cleanupInactiveDomain == true }.every { !it.vmExisted } :
                "Direct final-mode cleanup must wait until VM records are removed"
    }

    private void cleanupFirmwareVms(Throwable primary = null) {
        List<Throwable> failures = []
        firmwareVms.each { String uuid ->
            try {
                VmInstanceVO existing = dbFindByUuid(uuid, VmInstanceVO.class)
                if (existing != null) {
                    if (existing.state != VmInstanceState.Destroyed) {
                        destroyVmInstance { delegate.uuid = uuid }
                    }
                    expungeVmInstance { delegate.uuid = uuid }
                }
            } catch (Throwable error) {
                failures.add(error)
            }
            try {
                firmwareJobs(uuid).each { GarbageCollectorVO job ->
                    try {
                        if (job.status != GCStatus.Done) {
                            triggerFirmwareGc(job.uuid)
                            retryInSecs {
                                assert dbFindByUuid(job.uuid, GarbageCollectorVO.class).status == GCStatus.Done : "Owned firmware GC must finish before cleanup"
                            }
                        }
                        deleteGCJob { delegate.uuid = job.uuid }
                    } catch (Throwable error) {
                        failures.add(error)
                    }
                }
            } catch (Throwable error) {
                failures.add(error)
            }
        }
        if (!failures.empty) {
            Throwable error = primary ?: failures.remove(0)
            failures.each { error.addSuppressed(it) }
            if (primary == null) {
                throw error
            }
        }
    }

    void testRootVolumeRecordDeletedExpungeVmStillSuccess() {
        destroyVmInstance {
            uuid = vm.uuid
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Destroyed

        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vo.getRootVolumeUuid()).delete()
        assert !Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vo.getRootVolumeUuid()).exists

        expungeVmInstance {
            uuid = vm.uuid
        }
    }
}
