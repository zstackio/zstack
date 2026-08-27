package org.zstack.portal.managementnode

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.header.physicalserver.ManagedServiceResourceUsage
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.ResourceControlCommand
import org.zstack.header.physicalserver.ResourceControlResponse
import org.zstack.utils.data.SizeUnit

class LocalResourceControlExecutorCase {
    private LocalResourceControlExecutor executor
    private ResourceConsumerHandle restartable
    private ResourceConsumerHandle nonRestartable
    private boolean previousUnitTestOn

    @Before
    void setUp() {
        previousUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        CoreGlobalProperty.UNIT_TEST_ON = true
        executor = new LocalResourceControlExecutor()
        executor.setTestMode(true)
        restartable = systemdHandle(
                "prometheus", "prometheus.service", true)
        nonRestartable = systemdHandle(
                "management-node", "zstack-management.service", false)
    }

    @After
    void cleanUp() {
        executor?.setTestMode(false)
        CoreGlobalProperty.UNIT_TEST_ON = previousUnitTestOn
    }

    @Test
    void testAppliesAndInspectsCpuAndMemoryAsOneRoleBoundary() {
        ResourceControlCommand command = command(
                "APPLY", "3,1-2", SizeUnit.MEGABYTE.toByte(256),
                [restartable, nonRestartable])

        ResourceControlResponse response = executor.apply(command)

        assert response.cpuSet == "1-3" :
                "local execution must normalize the requested CPU set: " +
                        "expected=1-3 actual=${response.cpuSet}"
        assert response.memory == SizeUnit.MEGABYTE.toByte(256) :
                "the Role memory boundary must be preserved in platform bytes: " +
                        "actual=${response.memory}"
        assert response.coveredServiceCount == 2 &&
                response.expectedServiceCount == 2 :
                "every manifest handle must be covered by the Role boundary: " +
                        "actual=${response.coveredServiceCount}/${response.expectedServiceCount}"
        assert response.results*.state == ["READY", "READY"] :
                "an applied local Role must report every service ready: " +
                        "actual=${response.results*.state}"

        List<ManagedServiceResourceUsage> usage = executor.inspect(
                "MANAGEMENT", [restartable, nonRestartable])
        assert usage*.serviceName == ["prometheus", "management-node"] :
                "inspection must retain stable service identities: " +
                        "actual=${usage*.serviceName}"
        assert usage.every {
            it.state == "RUNNING" && it.cpuSet == "3,1-2" &&
                    it.memoryLimit == SizeUnit.MEGABYTE.toByte(256)
        } : "inspection must expose the last applied CPU and memory boundary: actual=${usage}"
    }

    @Test
    void testReleaseDisablesEveryHandleAndClearsMemoryLimit() {
        ResourceControlResponse response = executor.apply(command(
                "RELEASE", "0-3", SizeUnit.MEGABYTE.toByte(128),
                [restartable, nonRestartable]))

        assert response.cpuSet == "" :
                "release must remove the Role CPU constraint: actual=${response.cpuSet}"
        assert response.memory == 0L :
                "release must remove the Role memory limit: actual=${response.memory}"
        assert response.results*.state == ["DISABLED", "DISABLED"] :
                "release must disable every manifest handle: actual=${response.results*.state}"
    }

    @Test
    void testRejectsInvalidMemoryBeforeReportingSuccess() {
        Throwable failure = null
        try {
            executor.apply(command(
                    "APPLY", "0-1", 1L, [restartable]))
        } catch (Throwable error) {
            failure = error
        }

        assert failure?.message?.contains("MEMORY_LIMIT_INVALID") :
                "memory must use positive MiB-aligned platform bytes: actual=${failure}"
    }

    @Test
    void testRestartsOnlyExplicitRestartableSystemdHandles() {
        executor.restart([restartable])
        assert executor.lastTestRestartHandles*.value ==
                ["prometheus.service"] :
                "restart must use the stable systemd unit selected by the caller: " +
                        "actual=${executor.lastTestRestartHandles*.value}"

        assertRestartRejected([],
                "an empty service selection must never restart the whole Role")
        assertRestartRejected([nonRestartable],
                "a non-restartable service must remain outside Cloud restart ownership")

        ResourceConsumerHandle pidFile = new ResourceConsumerHandle(
                ResourceConsumerHandle.OWNER_PID_FILE,
                "/run/example.pid", "example", "example",
                false, true, "example")
        assertRestartRejected([pidFile],
                "restart supports stable systemd units only")
    }

    private void assertRestartRejected(
            List<ResourceConsumerHandle> handles, String intent) {
        Throwable failure = null
        try {
            executor.restart(handles)
        } catch (Throwable error) {
            failure = error
        }
        assert failure != null : "${intent}: expected rejection"
    }

    private static ResourceControlCommand command(
            String operation,
            String cpuSet,
            Long memory,
            List<ResourceConsumerHandle> handles) {
        ResourceControlCommand command = new ResourceControlCommand()
        command.roleType = "MANAGEMENT"
        command.isolationMode = "SHARED"
        command.operation = operation
        command.cpuSet = cpuSet
        command.memory = memory
        command.handles = handles
        return command
    }

    private static ResourceConsumerHandle systemdHandle(
            String serviceName, String unit, boolean restartable) {
        return new ResourceConsumerHandle(
                ResourceConsumerHandle.SYSTEMD_UNIT,
                unit, serviceName, serviceName,
                false, restartable, null)
    }
}
