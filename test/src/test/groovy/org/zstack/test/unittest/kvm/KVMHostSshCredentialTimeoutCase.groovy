package org.zstack.test.unittest.kvm

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.stubbing.Answer
import org.springframework.context.support.StaticMessageSource
import org.zstack.compute.host.HostBase
import org.zstack.compute.host.HostTracker
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.ansible.AnsibleFacade
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.config.GlobalConfig
import org.zstack.core.config.GlobalConfigException
import org.zstack.core.config.GlobalConfigFacadeImpl
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.thread.CancelablePeriodicTask
import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.DispatchQueue
import org.zstack.core.thread.SingleFlightTask
import org.zstack.core.thread.Task
import org.zstack.core.thread.ThreadFacade
import org.zstack.core.timeout.Timer
import org.zstack.header.core.Completion
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.HostErrors
import org.zstack.header.host.HostInventory
import org.zstack.header.host.HostStatusEvent
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMHost
import org.zstack.kvm.KVMHostVO
import org.zstack.utils.ShellUtils
import org.zstack.utils.ssh.SshResult
import org.zstack.utils.ssh.SshShell

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.function.Supplier
import javax.xml.bind.JAXBContext

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

/** Exercises the credential check and the actual HostBase queue/flow failure callbacks. */
class KVMHostSshCredentialTimeoutCase {
    private KVMHost host
    private ComponentLoader loader
    private Object oldLoader
    private Object oldMessageSource
    private Object oldTimeout
    private Object oldPortOpenTimeout
    private Object oldPortConnectTimeout
    private boolean oldUnitTestOn
    private Object asyncSafeAspect
    private Object oldAsyncSafeErrors
    private Object threadAspect
    private Object oldAspectThreads
    private Function<Supplier, Object> oldFactory
    private final List<SshShell> shells = []
    private final AtomicInteger passwordCalls = new AtomicInteger()
    private final AtomicInteger keyCalls = new AtomicInteger()
    private int passwordExit
    private int keyExit
    private boolean passwordTimeout
    private boolean keyTimeout
    private boolean runRealTimeout
    private CountDownLatch probeStarted
    private ExecutorService executor

    @Before
    void setUp() throws Exception {
        oldLoader = field(Platform.class, "loader").get(null)
        oldMessageSource = field(Platform.class, "messageSource").get(null)
        oldFactory = Platform.functionForMockTestObject
        oldTimeout = field(GlobalConfig.class, "value").get(KVMGlobalConfig.SSH_CREDENTIAL_CHECK_TIMEOUT)
        oldPortOpenTimeout = field(GlobalConfig.class, "value").get(KVMGlobalConfig.TEST_SSH_PORT_ON_OPEN_TIMEOUT)
        oldPortConnectTimeout = field(GlobalConfig.class, "value").get(KVMGlobalConfig.TEST_SSH_PORT_ON_CONNECT_TIMEOUT)
        oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        field(GlobalConfig.class, "value").set(KVMGlobalConfig.SSH_CREDENTIAL_CHECK_TIMEOUT, "1")

        loader = mock(ComponentLoader.class)
        ErrorFacade errors = mock(ErrorFacade.class)
        when(loader.getComponent(ErrorFacade.class)).thenReturn(errors)
        when(errors.instantiateErrorCode((Enum) any(Enum.class), (String) nullable(String.class),
                (ErrorCode) nullable(ErrorCode.class)))
                .thenAnswer { call ->
                    ErrorCode error = new ErrorCode(call.getArgument(0).toString(), "test", call.getArgument(1))
                    error.setCause(call.getArgument(2))
                    return error
                }
        when(errors.stringToInternalError(anyString())).thenAnswer { call ->
                new ErrorCode(SysErrors.INTERNAL.toString(), "test", call.getArgument(0))
        }
        Class<?> aspectClass = Class.forName("org.zstack.core.aspect.AsyncSafeAspect")
        asyncSafeAspect = aspectClass.getMethod("aspectOf").invoke(null)
        oldAsyncSafeErrors = field(aspectClass, "errf").get(asyncSafeAspect)
        field(aspectClass, "errf").set(asyncSafeAspect, errors)
        Class<?> threadAspectClass = Class.forName("org.zstack.core.aspect.ThreadAspect")
        threadAspect = threadAspectClass.getMethod("aspectOf").invoke(null)
        oldAspectThreads = field(threadAspectClass, "thread").get(threadAspect)
        field(Platform.class, "loader").set(null, loader)
        field(Platform.class, "messageSource").set(null, new StaticMessageSource())

        host = mock(KVMHost.class, CALLS_REAL_METHODS)
        KVMHostVO vo = new KVMHostVO()
        vo.setUuid("credential-check-host")
        vo.setManagementIp("127.0.0.1")
        vo.setUsername("test-user")
        vo.setPassword("test-password")
        vo.setPort(2200)
        field(HostBase.class, "self").set(host, vo)
        AnsibleFacade ansible = mock(AnsibleFacade.class)
        when(ansible.getPrivateKey()).thenReturn("test-private-key")
        field(KVMHost.class, "asf").set(host, ansible)

        Platform.functionForMockTestObject = { Supplier supplier ->
            Object object = supplier.get()
            if (!(object instanceof SshShell)) {
                return object
            }
            SshShell shell = new SshShell() {
                @Override
                SshResult runCommand(String command) {
                    assert false : "credential probe must use bounded execution"
                }

                @Override
                SshResult runCommand(String command, int timeoutSeconds) {
                    assert command == "echo hello"
                    assert timeoutSeconds == 1
                    assert getHostname() == "127.0.0.1"
                    assert getUsername() == "test-user"
                    assert getPort() == 2200
                    assert !(getWithSudo())
                    boolean key = getPrivateKey() != null
                    (key ? keyCalls : passwordCalls).incrementAndGet()
                    if (runRealTimeout && !key) {
                        probeStarted.countDown()
                        ShellUtils.runAndReturn("sleep 30", false, timeoutSeconds)
                        assert false : "the sleeping probe must time out"
                    }
                    if (key ? keyTimeout : passwordTimeout) {
                        throw new ShellUtils.ShellTimeoutException(command, timeoutSeconds)
                    }
                    SshResult result = new SshResult()
                    result.setReturnCode(key ? keyExit : passwordExit)
                    // Deliberately do not set isSshFailure: all nonzero exits must be checked.
                    return result
                }
            }
            shells.add(shell)
            return shell
        }
    }

    @After
    void tearDown() throws Exception {
        try {
            if (executor != null) {
                executor.shutdownNow()
                assert executor.awaitTermination(10, TimeUnit.SECONDS)
            }
        } finally {
            Platform.functionForMockTestObject = oldFactory
            field(GlobalConfig.class, "value").set(KVMGlobalConfig.SSH_CREDENTIAL_CHECK_TIMEOUT, oldTimeout)
            field(GlobalConfig.class, "value").set(KVMGlobalConfig.TEST_SSH_PORT_ON_OPEN_TIMEOUT, oldPortOpenTimeout)
            field(GlobalConfig.class, "value").set(KVMGlobalConfig.TEST_SSH_PORT_ON_CONNECT_TIMEOUT, oldPortConnectTimeout)
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
            field(Platform.class, "loader").set(null, oldLoader)
            field(Platform.class, "messageSource").set(null, oldMessageSource)
            if (asyncSafeAspect != null) {
                field(asyncSafeAspect.getClass(), "errf").set(asyncSafeAspect, oldAsyncSafeErrors)
            }
            if (threadAspect != null) {
                field(threadAspect.getClass(), "thread").set(threadAspect, oldAspectThreads)
            }
        }
    }

    @Test
    void credentialTimeoutLoadsEffectiveConfigAndValidatesBounds() {
        Field definition = KVMGlobalConfig.getDeclaredField("SSH_CREDENTIAL_CHECK_TIMEOUT")
        GlobalConfig original = (GlobalConfig) definition.get(null)
        GlobalConfigFacadeImpl facade = new GlobalConfigFacadeImpl()
        field(GlobalConfigFacadeImpl.class, "context").set(facade,
                JAXBContext.newInstance("org.zstack.core.config.schema"))
        // Test/API generation resources can override the production XML. Follow
        // startup's XML -> Java definitions -> validators -> link sequence.
        Class<?> initializerClass = Class.forName('org.zstack.core.config.GlobalConfigFacadeImpl$1GlobalConfigInitializer')
        Constructor<?> constructor = initializerClass.getDeclaredConstructor(GlobalConfigFacadeImpl.class)
        constructor.setAccessible(true)
        Object initializer = constructor.newInstance(facade)
        ((List) field(initializerClass, "globalConfigFields").get(initializer)).add(definition)
        Method parse = initializerClass.getDeclaredMethod("parseConfig", File.class)
        parse.setAccessible(true)
        parse.invoke(initializer, new File(getClass().classLoader.getResource("globalConfig/kvm.xml").toURI()))
        Method loadJava = initializerClass.getDeclaredMethod("loadConfigFromJava")
        loadJava.setAccessible(true)
        loadJava.invoke(initializer)
        Method createValidators = initializerClass.getDeclaredMethod("createValidatorForBothXmlAndDatabase")
        createValidators.setAccessible(true)
        createValidators.invoke(initializer)
        Method link = initializerClass.getDeclaredMethod("link", Field.class, GlobalConfig.class)
        link.setAccessible(true)
        try {
            link.invoke(initializer, definition, new GlobalConfig(original.getCategory(), original.getName()))
            GlobalConfig linked = (GlobalConfig) definition.get(null)
            assert linked.isLinked()
            assert linked.getType() == Integer.name
            assert linked.defaultValue(Integer.class) == 60
            assert linked.value(Integer.class) == 60
            for (String value : ["1", "60", "3600"]) {
                linked.validate(value)
            }
            for (String value : ["0", "-1", "3601", "2147483647", "2147483648", "invalid"]) {
                try {
                    linked.validate(value)
                    assert false : "accepted invalid credential timeout: ${value}"
                } catch (GlobalConfigException expected) {
                    // Invalid updates must fail before any SSH probe starts.
                }
            }
        } finally {
            definition.set(null, original)
        }
    }

    @Test
    void validPasswordContinuesWithoutTryingPrivateKey() {
        Result result = check()
        assert result.next == 1
        assert result.error == null
        assert keyCalls.get() == 0
        assert shells.get(0).getPassword() == "test-password"
    }

    @Test
    void passwordFailureAndWorkingKeyStillReportChangedPassword() {
        passwordExit = 5
        Result result = check()
        assert result.error.getCode() == HostErrors.HOST_PASSWORD_HAS_BEEN_CHANGED.toString()
        assert keyCalls.get() == 1
        assert shells.get(1).getPrivateKey() == "test-private-key"
    }

    @Test
    void bothFailuresKeepTheOriginalPasswordError() {
        passwordExit = 255
        keyExit = 255
        Result result = check()
        assert result.error.getCode() == HostErrors.UNABLE_TO_RECONNECT_HOST.toString()
        assert result.error.getDetails().contains("SSH password")
    }

    @Test
    void passwordTimeoutDoesNotTryKeyOrStopAutomaticReconnect() {
        passwordTimeout = true
        Result result = check()
        assert result.error.getCode() == SysErrors.TIMEOUT.toString()
        assert result.error.getDetails().contains("1 seconds")
        assert keyCalls.get() == 0
        assert !(Thread.currentThread().isInterrupted())
    }

    @Test
    void privateKeyTimeoutDoesNotBecomeChangedPassword() {
        passwordExit = 5
        keyTimeout = true
        Result result = check()
        assert result.error.getCode() == SysErrors.TIMEOUT.toString()
        assert keyCalls.get() == 1
    }

    @Test
    void nonSshErrorExitIsNotAcceptedAsSuccess() {
        passwordExit = 124
        keyExit = 127
        Result result = check()
        assert result.next == 0
        assert result.error.getCode() == HostErrors.UNABLE_TO_RECONNECT_HOST.toString()
    }

    @Test(timeout = 30000L)
    void timeoutReleasesBothQueuesAndAllWaitersAndAllowsRetry() throws Exception {
        CoreGlobalProperty.UNIT_TEST_ON = false
        field(GlobalConfig.class, "value").set(KVMGlobalConfig.TEST_SSH_PORT_ON_OPEN_TIMEOUT, "1")
        field(GlobalConfig.class, "value").set(KVMGlobalConfig.TEST_SSH_PORT_ON_CONNECT_TIMEOUT, "1")
        executor = Executors.newCachedThreadPool()
        ThreadFacade threads = mock(ThreadFacade.class)
        field(threadAspect.getClass(), "thread").set(threadAspect, threads)
        when(loader.getComponent(ThreadFacade.class)).thenReturn(threads)
        when(threads.submit(any(Task.class))).thenAnswer { call ->
            Task<?> task = call.getArgument(0)
            return executor.submit({ task.call() } as Callable)
        }
        Constructor<?> constructor = Class.forName("org.zstack.core.thread.DispatchQueueImpl").getDeclaredConstructor()
        constructor.setAccessible(true)
        DispatchQueue queue = (DispatchQueue) constructor.newInstance()
        field(queue.getClass(), "_threadFacade").set(queue, threads)
        Timer timer = mock(Timer.class)
        when(timer.getCurrentTimeMillis()).thenAnswer { System.currentTimeMillis() }
        field(queue.getClass(), "zTimer").set(queue, timer)
        when(threads.chainSubmit(any(ChainTask.class))).thenAnswer { call -> queue.chainSubmit(call.getArgument(0)) }
        when(threads.singleFlightSubmit(any(SingleFlightTask.class))).thenAnswer { call -> queue.singleFlightSubmit(call.getArgument(0)) }
        // Port readiness is an external prerequisite. Advance its real flow's
        // trigger without opening a socket; the credential flow runs normally.
        when(threads.submitCancelablePeriodicTask(any(CancelablePeriodicTask.class))).thenAnswer { call ->
            CancelablePeriodicTask task = call.getArgument(0)
            ((FlowTrigger) task.getBackups().get(0)).next()
            return CompletableFuture.completedFuture(null)
        }

        ConcurrentMap<String, CompletableFuture<ConnectHostReply>> pending = new ConcurrentHashMap<>()
        List<ConnectHostReply> replies = Collections.synchronizedList([])
        CloudBus bus = mock(CloudBus.class)
        doAnswer { call ->
            Message message = call.getArgument(0)
            ConnectHostReply reply = call.getArgument(1)
            replies.add(reply)
            assert pending.get(message.getId()).complete(reply) : "a connect message received more than one reply"
            return null
        }.when(bus).reply(any(Message.class), any(MessageReply.class))
        AtomicInteger disconnected = new AtomicInteger()
        AtomicInteger failureHooks = new AtomicInteger()
        KVMHost timeoutHost = connectHost("host-a", threads, bus, true, disconnected, failureHooks)
        KVMHost anotherHost = connectHost("host-b", threads, bus, false, new AtomicInteger(), new AtomicInteger())

        runRealTimeout = true
        probeStarted = new CountDownLatch(1)
        List<CompletableFuture<ConnectHostReply>> waiting = []
        waiting.add(submitConnect(timeoutHost, "host-a", pending))
        boolean started = probeStarted.await(5, TimeUnit.SECONDS)
        assert started : "credential probe was not reached; replies=" + replies
        for (int i = 0; i < 6; i++) {
            waiting.add(submitConnect(timeoutHost, "host-a", pending))
        }
        assert queue.getSingleFlightChainTaskInfo("connect-host-host-a-single-flight").getPendingTask().size() == 6

        assert submitConnect(anotherHost, "host-b", pending).get(1, TimeUnit.SECONDS).isSuccess() : "another host must run while host-a is blocked"
        awaitEmpty(queue, "host-b")
        for (CompletableFuture<ConnectHostReply> future : waiting) {
            ConnectHostReply reply = future.get(15, TimeUnit.SECONDS)
            assert !(reply.isSuccess())
            assert reply.getError().getCode() == SysErrors.TIMEOUT.toString()
        }
        awaitEmpty(queue, "host-a")
        assert passwordCalls.get() == 1
        assert replies.size() == 8 : "seven timeout replies and one other-host reply"
        assert disconnected.get() == 1 : "actual HostBase error handler updates connection state"
        assert failureHooks.get() == 1 : "actual HostBase error handler invokes its failure hook"

        runRealTimeout = false
        assert submitConnect(timeoutHost, "host-a", pending).get(5, TimeUnit.SECONDS).isSuccess()
        awaitEmpty(queue, "host-a")
        assert replies.size() == 9 : "later request receives exactly one reply"
        assert disconnected.get() == 1
        assert failureHooks.get() == 1
    }

    private KVMHost connectHost(String uuid, ThreadFacade threads, CloudBus bus, boolean checkCredentials,
                               AtomicInteger disconnected, AtomicInteger failureHooks) throws Exception {
        KVMHost target = mock(KVMHost.class, { call ->
            switch (call.getMethod().getName()) {
                case "getHostSyncLevel":
                    return 1
                case "changeConnectionState":
                    if (call.getArgument(0) == HostStatusEvent.disconnected) {
                        disconnected.incrementAndGet()
                    }
                    return true
                case "connectHostFailHook":
                    failureHooks.incrementAndGet()
                    return null
                case "getSelfInventory":
                    return mock(HostInventory.class)
                case "createPostConnectFlow":
                    return null
                case "connectHook":
                    ((Completion) call.getArgument(1)).success()
                    return null
                case "checkConnectConditions":
                    if (checkCredentials && runRealTimeout) {
                        return call.callRealMethod()
                    }
                    // Successful retry/other-host prerequisites are stubbed
                    // HostBase still runs its real success and finally handlers.
                    ((Completion) call.getArgument(1)).success()
                    return null
                default:
                    return call.callRealMethod()
            }
        } as Answer)
        KVMHostVO vo = new KVMHostVO()
        vo.setUuid(uuid)
        vo.setManagementIp("127.0.0.1")
        vo.setUsername("test-user")
        vo.setPassword("test-password")
        vo.setPort(2200)
        field(HostBase.class, "self").set(target, vo)
        field(HostBase.class, "thdf").set(target, threads)
        field(KVMHost.class, "thdf").set(target, threads)
        field(HostBase.class, "bus").set(target, bus)
        field(HostBase.class, "tracker").set(target, mock(HostTracker.class))
        field(HostBase.class, "pluginRgty").set(target, mock(PluginRegistry.class))
        DatabaseFacade database = mock(DatabaseFacade.class)
        when(database.reload(any())).thenAnswer { call -> call.getArgument(0) }
        field(HostBase.class, "dbf").set(target, database)
        field(KVMHost.class, "asf").set(target, field(KVMHost.class, "asf").get(host))
        return target
    }

    private CompletableFuture<ConnectHostReply> submitConnect(KVMHost target, String uuid,
            ConcurrentMap<String, CompletableFuture<ConnectHostReply>> pending) throws Exception {
        ConnectHostMsg message = new ConnectHostMsg(uuid)
        message.setCalledByAPI(true)
        CompletableFuture<ConnectHostReply> reply = new CompletableFuture<>()
        pending.put(message.getId(), reply)
        Method handle = HostBase.class.getDeclaredMethod("handle", ConnectHostMsg.class)
        handle.setAccessible(true)
        handle.invoke(target, message)
        return reply
    }

    private static void awaitEmpty(DispatchQueue queue, String hostId) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            if (queue.getSingleFlightChainTaskInfo("connect-host-" + hostId + "-single-flight") == null
                    && !queue.isChainTaskRunning("connect-host-" + hostId)) {
                return
            }
            Thread.sleep(10)
        }
        assert false : "host queues did not drain"
    }

    private Result check() {
        Result result = new Result()
        Method check = KVMHost.class.getDeclaredMethod("checkHostSshCredentials", FlowTrigger.class)
        check.setAccessible(true)
        check.invoke(host, result)
        assert result.next + result.failed == 1 : "exactly one terminal callback"
        return result
    }

    private static class Result implements FlowTrigger {
        int next
        int failed
        ErrorCode error
        @Override
        void fail(ErrorCode errorCode) {
            failed++
            error = errorCode
        }
        @Override
        void next() { next++ }
        @Override
        void setError(ErrorCode errorCode) { error = errorCode }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name)
        field.setAccessible(true)
        return field
    }
}
