package org.zstack.storage.ceph.backup

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.wiring.BeanWiringInfoResolver
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.wiring.BeanConfigurerSupport
import org.springframework.beans.factory.wiring.BeanWiringInfo
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.image.CancelDownloadImageMsg
import org.zstack.header.image.CancelDownloadImageReply
import org.zstack.header.image.ImageBackupStorageRefInventory
import org.zstack.header.image.ImageInventory
import org.zstack.header.message.MessageReply
import org.zstack.header.rest.JsonAsyncRESTCallback
import org.zstack.header.rest.RESTFacade
import org.zstack.header.storage.backup.GetImageDownloadProgressMsg
import org.zstack.header.storage.backup.GetImageDownloadProgressReply
import org.zstack.storage.ceph.CephMonBase
import org.zstack.storage.ceph.MonStatus
import org.zstack.utils.gson.JSONObjectUtil

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.util.*

import static org.junit.Assert.*
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

/** Exercises the real cancel handler and multi-mon HTTP callbacks without a database. */
class CephUploadCancelRecoveryCase {
    private static final String BS = "backup-storage"
    private static final String IMAGE = "image"
    private static final String PATH = "ceph://images/" + IMAGE
    private final List<String> cancelUrls = new ArrayList<>()
    private final List<GetImageDownloadProgressMsg> progressRequests = new ArrayList<>()
    private final List<CancelDownloadImageReply> replies = new ArrayList<>()
    private CephBackupStorageBase storage
    private CancelDownloadImageMsg request
    private MessageReply progress
    private int succeedOnCancel = Integer.MAX_VALUE
    private AnnotationBeanConfigurerAspect previousAspect
    private Object previousLoader

    @Before
    @SuppressWarnings(["unchecked", "rawtypes"])
    void setUp() throws Exception {
        previousAspect = AnnotationBeanConfigurerAspect.aspectOf()
        previousLoader = field(Platform.class, "loader").get(null)
        CloudBus bus = mock(CloudBus.class)
        RESTFacade rest = mock(RESTFacade.class)
        Thread testThread = Thread.currentThread()
        ErrorFacade errors = mock(ErrorFacade.class)
        ComponentLoader loader = mock(ComponentLoader.class, { call ->
            if (Thread.currentThread() == testThread && "getComponent".equals(call.getMethod().getName()) &&
                    call.getArguments().length == 1 && call.getArgument(0) == ErrorFacade.class) {
                return errors
            }
            if (previousLoader == null) {
                return RETURNS_DEFAULTS.answer(call)
            }
            try {
                return call.getMethod().invoke(previousLoader, call.getArguments())
            } catch (InvocationTargetException e) {
                throw e.getCause()
            }
        } as Answer)
        when(errors.instantiateErrorCode((Enum) any(Enum.class), (String) nullable(String.class),
                (ErrorCode) nullable(ErrorCode.class)))
                .thenAnswer({ call -> error(call.getArgument(0).toString(), call.getArgument(1)) } as Answer)
        field(Platform.class, "loader").set(null, loader)

        // Wire this test's real mons; retain existing wiring and service lookup for other threads.
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory()
        factory.addBeanPostProcessor(new BeanPostProcessor() {
            @Override
            Object postProcessBeforeInitialization(Object bean, String name) {
                if (bean instanceof CephMonBase) {
                    try {
                        field(CephMonBase.class, "restf").set(bean, rest)
                    } catch (Exception e) {
                        throw new AssertionError(e)
                    }
                }
                return bean
            }
        })
        AnnotationBeanConfigurerAspect aspect = new AnnotationBeanConfigurerAspect()
        aspect.setBeanFactory(factory)
        BeanConfigurerSupport wiring = (BeanConfigurerSupport) field(AnnotationBeanConfigurerAspect.class,
                "beanConfigurerSupport").get(aspect)
        wiring.setBeanWiringInfoResolver({ bean ->
            if (Thread.currentThread() == testThread && bean instanceof CephMonBase) {
                return new BeanWiringInfo(2, false)
            }
            previousAspect.configureBean(bean)
            return null
        } as BeanWiringInfoResolver)
        field(AnnotationBeanConfigurerAspect.class, 'ajc$perSingletonInstance').set(null, aspect)

        CephBackupStorageVO vo = new CephBackupStorageVO()
        vo.setUuid(BS)
        vo.setPoolName("images")
        vo.setFsid("fsid")
        Set<CephBackupStorageMonVO> mons = new LinkedHashSet<>()
        for (int i = 1; i <= 3; i++) {
            CephBackupStorageMonVO mon = new CephBackupStorageMonVO()
            mon.setUuid("mon-" + i)
            mon.setHostname("192.0.2." + i)
            mon.setStatus(MonStatus.Connected)
            mons.add(mon)
        }
        vo.setMons(mons)
        storage = mock(CephBackupStorageBase.class, CALLS_REAL_METHODS)
        field(storage.getClass(), "self").set(storage, vo)
        field(storage.getClass(), "bus").set(storage, bus)
        request = new CancelDownloadImageMsg()
        request.setBackupStorageUuid(BS)
        request.setCancellationApiId("upload-api")
        ImageInventory image = new ImageInventory()
        image.setUuid(IMAGE)
        image.setUrl("upload://image.iso")
        image.setBackupStorageRefs(Collections.singletonList(ref(BS, "http://192.0.2.2:7761/ceph/backupstorage/image/upload")))
        request.setImageInventory(image)
        progress = complete(PATH)

        doAnswer({ call ->
            cancelUrls.add(call.getArgument(0))
            JsonAsyncRESTCallback callback = call.getArgument(2)
            if (cancelUrls.size() == succeedOnCancel) {
                callback.success(new CephBackupStorageBase.AgentResponse())
            } else {
                callback.fail(error(SysErrors.OPERATION_ERROR.toString(), "no matched job to cancel"))
            }
            return null
        } as Answer).when(rest).asyncJsonPost(anyString(), (Object) any(Object.class),
                (JsonAsyncRESTCallback) any(JsonAsyncRESTCallback.class))
        doAnswer({ call ->
            progressRequests.add(call.getArgument(0))
            ((CloudBusCallBack) call.getArgument(1)).run(progress)
            return null
        } as Answer).when(bus).send((GetImageDownloadProgressMsg) any(GetImageDownloadProgressMsg.class),
                (CloudBusCallBack) any(CloudBusCallBack.class))
        doAnswer({ call -> replies.add(call.getArgument(1)); return null } as Answer)
                .when(bus).reply((CancelDownloadImageMsg) eq(request), (MessageReply) any(MessageReply.class))
    }

    @After
    void tearDown() throws Exception {
        field(AnnotationBeanConfigurerAspect.class, 'ajc$perSingletonInstance').set(null, previousAspect)
        field(Platform.class, "loader").set(null, previousLoader)
    }

    @Test
    void completedUploadIsReconciledAfterAllMonitorsRejectCancellation() {
        storage.handle(request)
        assertEquals(3, cancelUrls.size())
        assertEquals(1, progressRequests.size())
        assertEquals("192.0.2.2", progressRequests.get(0).getHostname())
        assertEquals(BS, progressRequests.get(0).getBackupStorageUuid())
        assertEquals(IMAGE, progressRequests.get(0).getImageUuid())
        assertTrue(reply().isSuccess())
        assertEquals(PATH, JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(reply()), Map.class).get("completedUploadInstallPath"))
    }

    @Test
    void taskOnAnotherMonitorIsCancelledWithoutFallback() {
        succeedOnCancel = 2
        storage.handle(request)
        assertEquals(2, cancelUrls.size())
        assertTrue(progressRequests.isEmpty())
        assertTrue(reply().isSuccess())
        assertNull(JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(reply()), Map.class).get("completedUploadInstallPath"))
    }

    @Test
    void incompleteUploadKeepsCancelFailure() {
        ((GetImageDownloadProgressReply) progress).setCompleted(false)
        assertFailedWithProgress()
    }

    @Test
    void failedProgressKeepsCancelFailure() {
        progress.setError(error(SysErrors.TIMEOUT.toString(), "progress timed out"))
        assertFailedWithProgress()
    }

    @Test
    void missingOwnerOrTaskKeepsCancelFailure() {
        progress = new MessageReply()
        progress.setError(error(SysErrors.RESOURCE_NOT_FOUND.toString(), "original mon or upload task missing"))
        assertFailedWithProgress()
    }

    @Test
    void unexpectedImagePathIsRejected() {
        progress = complete("ceph://images/another-image")
        assertFailedWithProgress()
    }

    @Test
    void missingCompletedImagePathIsRejected() {
        progress = complete(null)
        assertFailedWithProgress()
    }

    @Test
    void ordinaryDownloadDoesNotQueryUploadProgress() {
        request.getImageInventory().setUrl("https://example.com/image.iso")
        assertFailedWithoutProgress()
    }

    @Test
    void unrelatedStorageReferenceIsNotUsed() {
        request.getImageInventory().setBackupStorageRefs(Collections.singletonList(ref("other-bs", "http://192.0.2.1/upload")))
        assertFailedWithoutProgress()
    }

    @Test
    void invalidUploadAddressDoesNotGuessAMonitor() {
        request.getImageInventory().setBackupStorageRefs(Collections.singletonList(ref(BS, "http://invalid host/upload")))
        assertFailedWithoutProgress()
    }

    @Test
    void realCephInstallPathIsNotTreatedAsAnUploadMonitor() {
        request.getImageInventory().setBackupStorageRefs(Collections.singletonList(ref(BS, PATH)))
        assertFailedWithoutProgress()
    }

    @Test
    void alreadyReconciledUploadCanBeCancelledAgain() {
        ImageBackupStorageRefInventory reconciled = ref(BS, PATH)
        reconciled.setStatus("Ready")
        request.getImageInventory().setBackupStorageRefs(Collections.singletonList(reconciled))
        storage.handle(request)
        assertTrue(progressRequests.isEmpty())
        assertTrue(reply().isSuccess())
    }

    @Test
    void readyRefForAnotherImageDoesNotCountAsReconciled() {
        ImageBackupStorageRefInventory wrongImage = ref(BS, "ceph://images/another-image")
        wrongImage.setStatus("Ready")
        request.getImageInventory().setBackupStorageRefs(Collections.singletonList(wrongImage))
        assertFailedWithoutProgress()
    }

    @Test
    void readyRefForAnOrdinaryDownloadDoesNotCountAsReconciled() {
        request.getImageInventory().setUrl("https://example.com/image.iso")
        ImageBackupStorageRefInventory ordinary = ref(BS, PATH)
        ordinary.setStatus("Ready")
        request.getImageInventory().setBackupStorageRefs(Collections.singletonList(ordinary))
        assertFailedWithoutProgress()
    }

    @Test
    void ipv6UploadAddressSelectsItsOwner() {
        request.getImageInventory().setBackupStorageRefs(Collections.singletonList(ref(BS, "http://[2001:db8::2]:7761/upload")))
        storage.handle(request)
        assertEquals("2001:db8::2", progressRequests.get(0).getHostname())
        assertTrue(reply().isSuccess())
    }

    private void assertFailedWithProgress() {
        storage.handle(request)
        assertEquals(1, progressRequests.size())
        assertFailed()
    }

    private void assertFailedWithoutProgress() {
        storage.handle(request)
        assertTrue(progressRequests.isEmpty())
        assertFailed()
    }

    private void assertFailed() {
        assertFalse(reply().isSuccess())
        assertEquals("ORG_ZSTACK_STORAGE_CEPH_BACKUP_10001", reply().getError().getGlobalErrorCode())
        assertNull(JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(reply()), Map.class).get("completedUploadInstallPath"))
    }

    private CancelDownloadImageReply reply() {
        assertEquals("cancel must reply exactly once", 1, replies.size())
        return replies.get(0)
    }

    private static GetImageDownloadProgressReply complete(String path) {
        GetImageDownloadProgressReply reply = new GetImageDownloadProgressReply()
        reply.setCompleted(true)
        reply.setInstallPath(path)
        return reply
    }

    private static ImageBackupStorageRefInventory ref(String bs, String path) {
        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory()
        ref.setImageUuid(IMAGE)
        ref.setBackupStorageUuid(bs)
        ref.setInstallPath(path)
        ref.setStatus("Downloading")
        return ref
    }

    private static ErrorCode error(String code, String details) {
        ErrorCode err = new ErrorCode()
        err.setCode(code)
        err.setDetails(details)
        return err
    }

    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name)
                field.setAccessible(true)
                return field
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name)
    }
}
