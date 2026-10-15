package org.zstack.core.ansible

import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.timeout.ApiTimeoutManagerImpl
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.utils.TaskContext
import org.zstack.utils.ssh.Ssh
import org.zstack.utils.ssh.SshResult

import java.lang.reflect.Field
import java.lang.reflect.Proxy
import java.util.HashMap
import java.util.Map

import static org.junit.Assert.*

class AnsibleAgentRestartCase {
    private Object oldLoader
    private boolean oldUnitTestOn
    private Map<Object, Object> oldContext
    private Field loaderField

    @BeforeClass
    public static void initializePlatformForUnitTests() {
        String old = System.getProperty("unitTestOn")
        String oldIp = System.getProperty("management.server.ip")
        try {
            System.setProperty("unitTestOn", "true")
            if (oldIp == null) { System.setProperty("management.server.ip", "127.0.0.1"); }
            Class.forName(Platform.class.getName())
        } finally {
            if (old == null) { System.clearProperty("unitTestOn"); }
            else { System.setProperty("unitTestOn", old); }
            if (oldIp == null) { System.clearProperty("management.server.ip"); }
            else { System.setProperty("management.server.ip", oldIp); }
        }
    }

    private static class RestartSsh extends Ssh {
        SshResult response = new SshResult()
        int calls
        int remoteSeconds
        long localMillis
        int killAfter
        @Override public Ssh setExecTimeout(int seconds) { remoteSeconds = seconds; return this; }
        @Override public Ssh setExecTimeoutKillAfter(int seconds) { killAfter = seconds; return this; }
        @Override public Ssh setExecutionTimeout(long millis) { localMillis = millis; return this; }
        @Override public Ssh shell(String command) {
            assertTrue(remoteSeconds > 0 && localMillis > 0 && killAfter > 0)
            return this
        }
        @Override SshResult runAndClose() {
            calls++
            return response
        }
    }

    private static class Result extends Completion {
        int successes
        int failures
        ErrorCode error
        Result() { super(null); }
        @Override void success() { successes++; }
        @Override void fail(ErrorCode error) { failures++; this.error = error; }
    }

    @Before
    void setup() {
        oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        CoreGlobalProperty.UNIT_TEST_ON = false
        oldContext = TaskContext.getTaskContext() == null ? null : new HashMap<>(TaskContext.getTaskContext())
        TaskContext.removeTaskContext()
        loaderField = Platform.class.getDeclaredField("loader")
        loaderField.setAccessible(true)
        oldLoader = loaderField.get(null)
        ErrorFacade errors = (ErrorFacade) Proxy.newProxyInstance(getClass().getClassLoader(),
                [ErrorFacade.class] as Class[], { proxy, method, args ->
                        new ErrorCode("SYS.1006", "test", String.valueOf(args[1])) })
        ComponentLoader loader = (ComponentLoader) Proxy.newProxyInstance(getClass().getClassLoader(),
                [ComponentLoader.class] as Class[], { proxy, method, args ->
                        args != null && args[0] == ErrorFacade.class ? errors : null })
        loaderField.set(null, loader)
    }

    @After
    void cleanup() {
        loaderField.set(null, oldLoader)
        CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
        TaskContext.removeTaskContext()
        if (oldContext != null) { TaskContext.setTaskContext(oldContext); }
    }

    private AnsibleRunner runner(RestartSsh ssh) {
        AnsibleRunner runner = new AnsibleRunner() {
            @Override protected Ssh createRestartAgentSsh() { return ssh; }
        }
        Field asf = AnsibleRunner.class.getDeclaredField("asf")
        asf.setAccessible(true)
        asf.set(runner, Proxy.newProxyInstance(getClass().getClassLoader(), [AnsibleFacade.class] as Class[],
                { proxy, method, args -> null }))
        runner.setTargetIp("127.0.0.1")
        runner.setUsername("test")
        return runner
    }

    @Test
    void restartFailureCompletesOnceAndNextAttemptCanSucceed() {
        RestartSsh ssh = new RestartSsh()
        ssh.response.setReturnCode(-1)
        ssh.response.setSshFailure(true)
        ssh.response.setExitErrorMessage("SSH execution timed out")
        AnsibleRunner runner = runner(ssh)
        Result failed = new Result()
        runner.restartAgent("zstack-kvmagent", 120, failed)
        assertEquals(1, failed.failures)
        assertEquals(0, failed.successes)
        assertEquals("ORG_ZSTACK_CORE_ANSIBLE_10008", failed.error.getGlobalErrorCode())
        assertTrue(failed.error.getFormatArgs()[2].contains("timed out"))

        ssh.response = new SshResult()
        Result succeeded = new Result()
        runner.restartAgent("zstack-kvmagent", 120, succeeded)
        assertEquals(1, succeeded.successes)
        assertEquals(0, succeeded.failures)
        assertEquals(1, failed.failures)
        assertEquals(0, failed.successes)
        assertEquals(2, ssh.calls)
    }

    @Test
    void remoteRestartFailurePreservesExitStatusAndOutput() {
        RestartSsh ssh = new RestartSsh()
        ssh.response.setReturnCode(124)
        ssh.response.setStderr("agent restart exceeded its timeout")
        ssh.response.setStdout("waiting for agent to stop")
        Result result = new Result()
        runner(ssh).restartAgent("zstack-kvmagent", 120, result)
        assertEquals(1, ssh.calls)
        assertEquals(1, result.failures)
        assertEquals(0, result.successes)
        assertEquals("ORG_ZSTACK_CORE_ANSIBLE_10008", result.error.getGlobalErrorCode())
        assertEquals("zstack-kvmagent", result.error.getFormatArgs()[0])
        assertEquals("127.0.0.1", result.error.getFormatArgs()[1])
        String detail = result.error.getFormatArgs()[2]
        assertTrue(detail.contains("exit code[124]"))
        assertTrue(detail.contains("agent restart exceeded its timeout"))
        assertTrue(detail.contains("waiting for agent to stop"))
    }

    @Test
    void inheritedDeadlineCapsLocalAndRemoteBudgets() {
        TaskContext.putTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_DEADLINE,
                String.valueOf(System.currentTimeMillis() + 30000))
        RestartSsh ssh = new RestartSsh()
        Result result = new Result()
        runner(ssh).restartAgent("zstack-kvmagent", 120, result)
        assertEquals(1, result.successes)
        assertTrue(ssh.localMillis <= 30000 && ssh.localMillis > 20000)
        assertTrue((ssh.remoteSeconds + ssh.killAfter) * 1000L < ssh.localMillis)
    }

    @Test
    void insufficientInheritedDeadlineDoesNotStartAnotherRemoteRestart() {
        for (long remainingMillis : [-1L, 21000L]) {
            TaskContext.putTaskContextItem(ApiTimeoutManagerImpl.TASK_CONTEXT_MESSAGE_DEADLINE,
                    String.valueOf(System.currentTimeMillis() + remainingMillis))
            RestartSsh ssh = new RestartSsh()
            Result result = new Result()
            runner(ssh).restartAgent("zstack-kvmagent", 120, result)
            assertEquals(0, ssh.calls)
            assertEquals(1, result.failures)
            assertEquals(0, result.successes)
            assertEquals("ORG_ZSTACK_CORE_ANSIBLE_10008", result.error.getGlobalErrorCode())
            assertTrue(result.error.getFormatArgs()[2].contains("Insufficient time remaining"))
        }
    }

    @Test
    void insufficientBudgetDoesNotStartRemoteRestart() {
        for (int seconds : [0, 6, 7, 20, 21]) {
            RestartSsh ssh = new RestartSsh()
            Result result = new Result()
            runner(ssh).restartAgent("zstack-kvmagent", seconds, result)
            assertEquals(0, ssh.calls)
            assertEquals(1, result.failures)
            assertEquals(0, result.successes)
            assertEquals("ORG_ZSTACK_CORE_ANSIBLE_10008", result.error.getGlobalErrorCode())
        }
    }

    @Test
    void configuredBudgetAppliesWithoutParentDeadline() {
        assertEquals(120000, AnsibleRunner.restartTimeoutMillis(120))
        assertEquals(300000, AnsibleRunner.restartTimeoutMillis(300))
        RestartSsh ssh = new RestartSsh()
        Result result = new Result()
        runner(ssh).restartAgent("zstack-kvmagent", 22, result)
        assertEquals(1, ssh.calls)
        assertEquals(22000, ssh.localMillis)
        assertEquals(1, result.successes)
        assertEquals(0, result.failures)
    }
}
