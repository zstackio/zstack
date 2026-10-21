package org.zstack.test.integration.kvm.host

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.ansible.AnsibleRunner
import org.zstack.core.db.Q
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.header.core.Completion
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.core.workflow.NoRollbackFlow
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ReconnectHostAction
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.ssh.Ssh
import org.zstack.utils.ssh.SshResult

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** ZSTAC-73486: a failed fake-dead restart must finish the host reconnect flow. */
class KvmAgentFakeDeadReconnectCase extends SubCase {
    EnvSpec env
    HostInventory host
    RestartState state = new RestartState()

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm")
            installRestartSimulation()
            String oldEnabled = KVMGlobalConfig.RESTART_AGENT_IF_FAKE_DEAD.value()
            String oldTimeout = KVMGlobalConfig.AGENT_RESTART_TIMEOUT.value()
            String oldPingInterval = HostGlobalConfig.PING_HOST_INTERVAL.value()
            try {
                // Keep background tracking from starting another reconnect
                // while this case checks the exact restart/callback counts.
                HostGlobalConfig.PING_HOST_INTERVAL.updateValue(3600)
                KVMGlobalConfig.RESTART_AGENT_IF_FAKE_DEAD.updateValue(true)
                KVMGlobalConfig.AGENT_RESTART_TIMEOUT.updateValue(31)
                testRestartFailureThenReconnect()
                testRestartDisabled()
            } finally {
                KVMGlobalConfig.RESTART_AGENT_IF_FAKE_DEAD.updateValue(oldEnabled)
                KVMGlobalConfig.AGENT_RESTART_TIMEOUT.updateValue(oldTimeout)
                HostGlobalConfig.PING_HOST_INTERVAL.updateValue(oldPingInterval)
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    private void installRestartSimulation() {
        // Run the actual echo flow even though the other host deployment steps
        // remain skipped in test mode. Do not replace the reconnect workflow.
        env.mockFactory(NoRollbackFlow.class) { NoRollbackFlow original ->
            def name = original.class.declaredFields.find { it.name == "__name__" }
            if (name == null) {
                return original
            }
            name.accessible = true
            if (name.get(original) != "echo-host") {
                return original
            }
            return new NoRollbackFlow() {
                String __name__ = "echo-host"
                @Override
                void run(FlowTrigger trigger, Map data) {
                    original.run(trigger, data)
                }
            }
        }
        env.mockFactory(AnsibleRunner.class) { AnsibleRunner ignored ->
            return new RestartRunner(state)
        }
        env.simulator(KVMConstant.KVM_ECHO_PATH) {
            state.echoCalls.incrementAndGet()
            if (!state.agentResponsive) {
                throw new IllegalStateException("ZSTAC-73486 simulated fake-dead agent")
            }
            return [:]
        }
    }

    private void testRestartFailureThenReconnect() {
        for (boolean sshTimeout : [true, false]) {
            state.reset()
            state.response.returnCode = sshTimeout ? 1 : 124
            state.response.sshFailure = sshTimeout
            state.response.exitErrorMessage = sshTimeout ? "SSH execution timed out" : null
            state.response.stderr = sshTimeout ? "" : "agent stop exceeded its timeout"

            ReconnectHostAction.Result failed = reconnect()
            assert failed.error != null
            def error = failed.error
            while (error != null && error.globalErrorCode != "ORG_ZSTACK_CORE_ANSIBLE_10008") {
                error = error.cause
            }
            assert error != null : "reconnect must retain the agent restart error: ${failed.error}"
            assert error.details.contains(sshTimeout ? "SSH execution timed out" : "agent stop exceeded its timeout")
            assertHostStatus(HostStatus.Disconnected)
            assert state.echoCalls.get() > 0
            assert state.restartCalls.get() == 1
            assert state.failedCallbacks.get() == 1
            assert state.successCallbacks.get() == 0
            assertRestartBudget()

            // A second API on the same host must run after the failure, proving
            // that the previous reconnect released the host task queue.
            state.reset()
            ReconnectHostAction.Result recovered = reconnect()
            assert recovered.error == null
            assertHostStatus(HostStatus.Connected)
            assert state.echoCalls.get() >= 2
            assert state.restartCalls.get() == 1
            assert state.failedCallbacks.get() == 0
            assert state.successCallbacks.get() == 1
            assert state.agentResponsive
            assertRestartBudget()
        }
    }

    private void testRestartDisabled() {
        KVMGlobalConfig.RESTART_AGENT_IF_FAKE_DEAD.updateValue(false)
        state.reset()
        ReconnectHostAction.Result failed = reconnect()
        assert failed.error != null
        assertHostStatus(HostStatus.Disconnected)
        assert state.echoCalls.get() > 0
        assert state.restartCalls.get() == 0
        assert state.failedCallbacks.get() == 0
        assert state.successCallbacks.get() == 0

        state.agentResponsive = true
        assert reconnect().error == null
        assertHostStatus(HostStatus.Connected)
        assert state.restartCalls.get() == 0
    }

    private ReconnectHostAction.Result reconnect() {
        ReconnectHostAction action = new ReconnectHostAction()
        action.uuid = host.uuid
        action.sessionId = adminSession()
        // Longer than the configured restart budget so the API deadline does
        // not itself cause the simulated restart failure.
        action.timeout = TimeUnit.SECONDS.toMillis(60)
        return action.call()
    }

    private void assertHostStatus(HostStatus expected) {
        assert Q.New(HostVO.class).eq(HostVO_.uuid, host.uuid)
                .select(HostVO_.status).findValue() == expected
    }

    private void assertRestartBudget() {
        assert state.timeoutSeconds == 31
        assert state.sshBudgetMillis > 0 && state.sshBudgetMillis <= 31000
        assert state.script == "sudo bash /etc/init.d/zstack-kvmagent restart\n"
        assert CoreGlobalProperty.UNIT_TEST_ON
    }

    private static class RestartState {
        final AtomicInteger echoCalls = new AtomicInteger()
        final AtomicInteger restartCalls = new AtomicInteger()
        final AtomicInteger failedCallbacks = new AtomicInteger()
        final AtomicInteger successCallbacks = new AtomicInteger()
        volatile boolean agentResponsive
        volatile SshResult response
        volatile int timeoutSeconds
        volatile long sshBudgetMillis
        volatile String script

        void reset() {
            echoCalls.set(0)
            restartCalls.set(0)
            failedCallbacks.set(0)
            successCallbacks.set(0)
            agentResponsive = false
            response = new SshResult()
            timeoutSeconds = 0
            sshBudgetMillis = 0
            script = null
        }
    }

    private static class RestartRunner extends AnsibleRunner {
        private final RestartState state

        RestartRunner(RestartState state) {
            this.state = state
        }

        @Override
        protected Ssh createRestartAgentSsh() {
            return new Ssh() {
                @Override
                Ssh setExecutionTimeout(long milliseconds) {
                    state.sshBudgetMillis = milliseconds
                    return super.setExecutionTimeout(milliseconds)
                }

                @Override
                Ssh shell(String command) {
                    state.script = command
                    return this
                }

                @Override
                SshResult runAndClose() {
                    if (state.response.returnCode == 0 && !state.response.sshFailure) {
                        state.agentResponsive = true
                    }
                    return state.response
                }
            }
        }

        @Override
        void restartAgent(String agentName, int timeoutSeconds, Completion completion) {
            state.restartCalls.incrementAndGet()
            state.timeoutSeconds = timeoutSeconds
            boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
            Completion forward = new Completion(completion) {
                @Override
                void success() {
                    CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
                    state.successCallbacks.incrementAndGet()
                    completion.success()
                }

                @Override
                void fail(ErrorCode error) {
                    CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
                    state.failedCallbacks.incrementAndGet()
                    completion.fail(error)
                }
            }
            // Only the real bounded restart method runs outside test mode.
            // Its SSH transport is simulated, so it returns synchronously.
            CoreGlobalProperty.UNIT_TEST_ON = false
            try {
                super.restartAgent(agentName, timeoutSeconds, forward)
            } finally {
                CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
            }
        }
    }
}
