package org.zstack.test.integration.storage.backup.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.agent.AgentConstant
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQLBatch
import org.zstack.header.image.*
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor
import org.zstack.header.rest.RESTFacade
import org.zstack.header.storage.backup.*
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.LongJobInventory
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.backup.CephBackupStorageVO
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CephUploadImageCancelCase extends SubCase {
    static final long RESERVED_SIZE = 4096L
    static final String OWNER_HOST = "127.0.0.2"
    static final String UPLOAD_URL = "http://${OWNER_HOST}:7761/ceph/backupstorage/image/upload"

    EnvSpec env
    CloudBus bus
    BackupStorageInventory bs
    String oldMaxIdle
    Runnable closeRestInterceptor
    EventCallback trackEvents

    volatile String imageUuid
    volatile String installPath
    volatile String progressMode = "incomplete"
    volatile boolean secondMonCancels
    AtomicInteger cancelCalls = new AtomicInteger()
    AtomicInteger progressCalls = new AtomicInteger()
    AtomicInteger allocations = new AtomicInteger()
    AtomicInteger trackSuccesses = new AtomicInteger()
    List<String> cancelHosts = Collections.synchronizedList([])
    List<String> progressHosts = Collections.synchronizedList([])
    List<String> deletedPaths = Collections.synchronizedList([])
    List<Long> returnedSizes = Collections.synchronizedList([])
    List<Map> cleanupSnapshots = Collections.synchronizedList([])

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            cephBackupStorage {
                name = "ceph"
                url = "/ceph"
                fsid = "1fb9b14d-11a4-42af-bdfb-18c3f41d97d9"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                monUrls = ["root:password@127.0.0.1/?monPort=7777",
                           "root:password@127.0.0.2/?monPort=7777"]
            }
        }
    }

    @Override
    void test() {
        env.create {
            bs = env.inventoryByName("ceph") as BackupStorageInventory
            bus = bean(CloudBus.class)
            oldMaxIdle = ImageGlobalConfig.UPLOAD_MAX_IDLE_IN_SECONDS.value(String.class)
            // Exercise the real inactivity/suspend path with a shorter wait than the default 30 seconds.
            ImageGlobalConfig.UPLOAD_MAX_IDLE_IN_SECONDS.updateValue("6")
            installObservers()
            installSimulators()
            testCompletedUploadCancellation(false)
            testCompletedUploadCancellation(true)
            testCompletedUploadCancellation(false, true)
            testNormalMultiMonCancellation()
        }
    }

    void installObservers() {
        closeRestInterceptor = bean(RESTFacade.class).installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
            }

            @Override
            void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
                if (url.endsWith(CephBackupStorageBase.GET_DOWNLOAD_PROGRESS_PATH)) {
                    progressHosts.add(new URI(url).host)
                } else if (url.endsWith(AgentConstant.CANCEL_JOB)) {
                    cancelHosts.add(new URI(url).host)
                }
            }
        })

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            void beforeDeliveryMessage(Message msg) {
                if (msg instanceof AllocateBackupStorageMsg && msg.backupStorageUuid == bs.uuid) {
                    allocations.incrementAndGet()
                } else if (msg instanceof ReturnBackupStorageMsg && msg.backupStorageUuid == bs.uuid) {
                    returnedSizes.add(msg.size)
                } else if (msg instanceof ImageDeletionMsg && msg.imageUuid == imageUuid) {
                    ImageVO image = dbFindByUuid(imageUuid, ImageVO.class)
                    ImageBackupStorageRefVO ref = findRef()
                    cleanupSnapshots.add([imageStatus: image?.status, actualSize: image?.actualSize,
                                          refStatus: ref?.status, installPath: ref?.installPath])
                }
            }
        }, AllocateBackupStorageMsg.class, ReturnBackupStorageMsg.class, ImageDeletionMsg.class)

        trackEvents = new EventCallback<ImageCanonicalEvents.ImageTrackData>() {
            @Override
            protected void run(Map<String, String> tokens, ImageCanonicalEvents.ImageTrackData data) {
                if (data.uuid == imageUuid && data.success) {
                    trackSuccesses.incrementAndGet()
                }
            }
        }
        bean(EventFacade.class).on(ImageCanonicalEvents.IMAGE_TRACK_RESULT_PATH, trackEvents)
    }

    void installSimulators() {
        env.afterSimulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) { rsp ->
            rsp.size = 0L
            rsp.actualSize = 0L
            rsp.uploadPath = UPLOAD_URL
            return rsp
        }

        env.simulator(CephBackupStorageBase.GET_DOWNLOAD_PROGRESS_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.GetDownloadProgressCmd.class)
            assert cmd.imageUuid == imageUuid
            progressCalls.incrementAndGet()
            def rsp = new CephBackupStorageBase.GetDownloadProgressRsp()
            rsp.actualSize = RESERVED_SIZE
            rsp.size = RESERVED_SIZE * 2
            rsp.downloadSize = RESERVED_SIZE / 2
            rsp.lastOpTime = 1L
            rsp.progress = 50
            if (progressMode == "failed") {
                rsp.success = false
                rsp.error = "upload progress unavailable"
            } else if (progressMode == "completed" || progressMode == "invalid-path") {
                rsp.completed = true
                rsp.downloadSize = RESERVED_SIZE
                // Completion metadata must not change the capacity previously reserved by the tracker.
                rsp.actualSize = RESERVED_SIZE * 3
                rsp.installPath = progressMode == "completed" ? installPath : "ceph://wrong-pool/${imageUuid}"
            }
            return rsp
        }

        env.simulator(AgentConstant.CANCEL_JOB) {
            int call = cancelCalls.incrementAndGet()
            def rsp = new CephBackupStorageBase.AgentResponse()
            rsp.success = secondMonCancels && call == 2
            if (!rsp.success) {
                rsp.error = "no matched job to cancel"
            }
            return rsp
        }

        env.afterSimulator(CephBackupStorageBase.DELETE_IMAGE_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.DeleteCmd.class)
            deletedPaths.add(cmd.installPath)
            return rsp
        }
    }

    void testCompletedUploadCancellation(boolean retryCanceling, boolean rejectReconciliation = false) {
        resetObservations()
        imageUuid = Platform.uuid
        installPath = "ceph://${dbFindByUuid(bs.uuid, CephBackupStorageVO.class).poolName}/${imageUuid}"
        progressMode = "incomplete"
        long availableBefore = dbFindByUuid(bs.uuid, BackupStorageVO.class).availableCapacity

        APIAddImageMsg add = new APIAddImageMsg()
        add.resourceUuid = imageUuid
        add.name = "suspended-upload-${retryCanceling}"
        add.url = "upload://test.qcow2"
        add.format = ImageConstant.QCOW2_FORMAT_STRING
        add.mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
        add.platform = ImagePlatform.Linux.toString()
        add.backupStorageUuids = [bs.uuid]
        LongJobInventory job = submitLongJob {
            jobName = APIAddImageMsg.simpleName
            jobData = JSONObjectUtil.toJsonString(add)
        } as LongJobInventory

        retryInSecs(30) {
            assert dbFindByUuid(job.uuid, LongJobVO.class).state == LongJobState.Suspended
            assert dbFindByUuid(imageUuid, ImageVO.class).actualSize == RESERVED_SIZE
            assert dbFindByUuid(bs.uuid, BackupStorageVO.class).availableCapacity == availableBefore - RESERVED_SIZE
        }
        assert findRef().installPath == UPLOAD_URL
        assert findRef().status == ImageStatus.Downloading
        assert dbFindByUuid(imageUuid, ImageVO.class).status == ImageStatus.Downloading
        assert progressHosts.every { it == OWNER_HOST }
        assert allocations.get() == 1

        ImageInventory inventory = ImageInventory.valueOf(dbFindByUuid(imageUuid, ImageVO.class))
        if (retryCanceling) {
            // An unfinished upload must retain the cancellation failure and be retryable from Canceling.
            expect(AssertionError.class) {
                cancelLongJob { uuid = job.uuid }
            }
            assert dbFindByUuid(job.uuid, LongJobVO.class).state == LongJobState.Canceling
            assert findRef().status == ImageStatus.Downloading
        }

        ["incomplete", "failed", "invalid-path"].each { String mode ->
            progressMode = mode
            int beforeProgress = progressCalls.get()
            CancelDownloadImageReply reply = cancelDownload(inventory, job.apiId)
            assert !reply.success
            assert reply.completedUploadInstallPath == null
            assert reply.error.globalErrorCode == "ORG_ZSTACK_STORAGE_CEPH_BACKUP_10001"
            assert progressCalls.get() == beforeProgress + 1
            assert findRef().status == ImageStatus.Downloading
            assert findRef().installPath == UPLOAD_URL
        }

        inventory.url = "http://example.com/image.qcow2"
        int beforeNonUpload = progressCalls.get()
        assert !cancelDownload(inventory, job.apiId).success
        assert progressCalls.get() == beforeNonUpload
        assert cleanupSnapshots.empty
        assert deletedPaths.empty
        assert returnedSizes.empty

        // The agent finishes after the tracker has stopped, then removes its task daemon.
        progressMode = "completed"
        if (rejectReconciliation) {
            assertFailedReconciliationRetainsImage(job)
        }
        if (retryCanceling) {
            // Persist the reconciliation without completing the long job, as when its reply is lost.
            AddImageMsg amsg = new AddImageMsg()
            amsg.resourceUuid = imageUuid
            amsg.backupStorageUuids = [bs.uuid]
            CancelAddImageMsg msg = new CancelAddImageMsg()
            msg.msg = amsg
            msg.imageUuid = imageUuid
            msg.cancellationApiId = job.apiId
            bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imageUuid)
            assert bus.call(msg).success
            assert findRef().status == ImageStatus.Ready
            assert findRef().installPath == installPath
            assert dbFindByUuid(imageUuid, ImageVO.class).status == ImageStatus.Downloading
            assert dbFindByUuid(job.uuid, LongJobVO.class).state == LongJobState.Canceling
            assert deletedPaths.empty
            assert returnedSizes.empty
        }
        cancelCalls.set(0)
        cancelHosts.clear()
        int beforeCompleted = progressCalls.get()
        int allocationsBeforeCancel = allocations.get()
        cancelLongJob { uuid = job.uuid }

        retryInSecs {
            assert dbFindByUuid(job.uuid, LongJobVO.class).state == LongJobState.Canceled
            assert dbFindByUuid(imageUuid, ImageVO.class) == null
            assert findRef() == null
            assert dbFindByUuid(bs.uuid, BackupStorageVO.class).availableCapacity == availableBefore
            assert returnedSizes == [RESERVED_SIZE]
        }
        assert cancelCalls.get() == 2
        assert cancelHosts.toSet() == ["127.0.0.1", "127.0.0.2"].toSet()
        assert progressCalls.get() == beforeCompleted + (retryCanceling ? 0 : 1)
        assert progressHosts.every { it == OWNER_HOST }
        assert deletedPaths == [installPath]
        assert cleanupSnapshots == [[imageStatus: ImageStatus.Downloading, actualSize: RESERVED_SIZE,
                                     refStatus: ImageStatus.Ready, installPath: installPath]]
        assert allocations.get() == allocationsBeforeCancel
        assert trackSuccesses.get() == 0

        cancelLongJob { uuid = job.uuid }
        assert dbFindByUuid(job.uuid, LongJobVO.class).state == LongJobState.Canceled
        assert cancelCalls.get() == 2
        assert deletedPaths == [installPath]
        assert returnedSizes == [RESERVED_SIZE]
        assert allocations.get() == allocationsBeforeCancel
    }

    void assertFailedReconciliationRetainsImage(LongJobInventory job) {
        String triggerName = "zstac59112_${imageUuid}"
        // Reject only this image's reconciliation so the real transaction exception reaches AsyncBackupAspect.
        executeNativeSql("""
CREATE TRIGGER ${triggerName} BEFORE UPDATE ON ImageBackupStorageRefVO
FOR EACH ROW BEGIN
    IF NEW.imageUuid = '${imageUuid}' AND NEW.backupStorageUuid = '${bs.uuid}'
       AND OLD.status = 'Downloading' AND NEW.status = 'Ready' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'reject upload cancellation reconciliation';
    END IF;
END
""")
        try {
            expect(AssertionError.class) {
                cancelLongJob { uuid = job.uuid }
            }
            assert dbFindByUuid(job.uuid, LongJobVO.class).state == LongJobState.Canceling
            assert findRef().status == ImageStatus.Downloading
            assert findRef().installPath == UPLOAD_URL
            assert dbFindByUuid(imageUuid, ImageVO.class).actualSize == RESERVED_SIZE
            assert cleanupSnapshots.empty
            assert deletedPaths.empty
            assert returnedSizes.empty
        } finally {
            executeNativeSql("DROP TRIGGER IF EXISTS ${triggerName}")
        }
    }

    void executeNativeSql(String statement) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                databaseFacade.entityManager.createNativeQuery(statement).executeUpdate()
            }
        }.execute()
    }

    void testNormalMultiMonCancellation() {
        cancelCalls.set(0)
        cancelHosts.clear()
        secondMonCancels = true
        int beforeProgress = progressCalls.get()
        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory()
        ref.backupStorageUuid = bs.uuid
        ref.installPath = UPLOAD_URL
        ImageInventory inventory = new ImageInventory()
        inventory.uuid = Platform.uuid
        inventory.url = "upload://in-progress.qcow2"
        inventory.backupStorageRefs = [ref]

        CancelDownloadImageReply reply = cancelDownload(inventory, Platform.uuid)
        assert reply.success
        assert reply.completedUploadInstallPath == null
        assert cancelCalls.get() == 2
        assert cancelHosts.toSet() == ["127.0.0.1", "127.0.0.2"].toSet()
        assert progressCalls.get() == beforeProgress
    }

    CancelDownloadImageReply cancelDownload(ImageInventory inventory, String apiId) {
        CancelDownloadImageMsg msg = new CancelDownloadImageMsg()
        msg.imageInventory = inventory
        msg.backupStorageUuid = bs.uuid
        msg.cancellationApiId = apiId
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, bs.uuid)
        return bus.call(msg) as CancelDownloadImageReply
    }

    ImageBackupStorageRefVO findRef() {
        return Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, imageUuid)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, bs.uuid).find()
    }

    void resetObservations() {
        cancelCalls.set(0)
        progressCalls.set(0)
        allocations.set(0)
        trackSuccesses.set(0)
        cancelHosts.clear()
        progressHosts.clear()
        deletedPaths.clear()
        returnedSizes.clear()
        cleanupSnapshots.clear()
        secondMonCancels = false
    }

    @Override
    void clean() {
        closeRestInterceptor?.run()
        if (trackEvents != null) {
            bean(EventFacade.class).off(trackEvents)
        }
        if (oldMaxIdle != null) {
            ImageGlobalConfig.UPLOAD_MAX_IDLE_IN_SECONDS.updateValue(oldMaxIdle)
        }
        env.delete()
    }
}
