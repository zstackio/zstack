package org.zstack.portal.managementnode

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.header.physicalserver.ManagedServiceResourceUsage
import org.zstack.header.physicalserver.ResourceConsumerHandle
import org.zstack.header.physicalserver.ResourceControlCommand
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
        executor.enableTestMode()
        restartable = restartableSystemdHandle("prometheus", "prometheus.service")
        nonRestartable = systemdHandle("management-node", "zstack-management.service")
    }

    @After
    void cleanUp() {
        executor?.disableTestMode()
        CoreGlobalProperty.UNIT_TEST_ON = previousUnitTestOn
    }

    @Test
    void testAppliesAndInspectsCpuAndMemoryAsOneRoleBoundary() {
        ResourceControlCommand command = command("3,1-2", SizeUnit.MEGABYTE.toByte(256), [restartable, nonRestartable])

        boolean response = executor.apply(command)

        assert response :
                "Apply may report Synced only after every manifest handle has the requested boundary"

        List<ManagedServiceResourceUsage> usage = executor.inspect("MANAGEMENT", [restartable, nonRestartable])
        assert usage*.serviceName == ["prometheus", "management-node"] :
                "inspection must retain stable service identities: " + "actual=${usage*.serviceName}"
        assert usage.every {
            it.state == "RUNNING" && it.cpuSet == "3,1-2" && it.memoryLimit == SizeUnit.MEGABYTE.toByte(256)
        } : "inspection must expose the last applied CPU and memory boundary: actual=${usage}"
    }

    @Test
    void testReleaseDisablesEveryHandleAndClearsMemoryLimit() {
        boolean response = executor.release(command(
                "0-3", SizeUnit.MEGABYTE.toByte(128), [restartable, nonRestartable]))

        assert response :
                "Release may report Synced only after every manifest handle is disabled"
    }

    @Test
    void testRejectsInvalidMemoryBeforeReportingSuccess() {
        Throwable failure = null
        try {
            executor.apply(command("0-1", 1L, [restartable]))
        } catch (Throwable error) {
            failure = error
        }

        assert failure?.message?.contains("positive multiple of 1 MiB") :
                "memory must use positive MiB-aligned platform bytes: actual=${failure}"
    }

    @Test
    void testRestartsOnlyExplicitRestartableSystemdHandles() {
        executor.restart("zstack-management.slice", [restartable])
        assert executor.lastTestRestartHandles*.value == ["prometheus.service"] :
                "restart must use the stable systemd unit selected by the caller: " +
                        "actual=${executor.lastTestRestartHandles*.value}"

        assertRestartRejected([], "an empty service selection must never restart the whole Role")
        assertRestartRejected([nonRestartable], "a non-restartable service must remain outside Cloud restart ownership")
    }

    private void assertRestartRejected(List<ResourceConsumerHandle> handles, String intent) {
        Throwable failure = null
        try {
            executor.restart("zstack-management.slice", handles)
        } catch (Throwable error) {
            failure = error
        }
        assert failure != null : "${intent}: expected rejection"
    }

    private static ResourceControlCommand command(String cpuSet, Long memory, List<ResourceConsumerHandle> handles) {
        ResourceControlCommand command = new ResourceControlCommand()
        command.roleType = "MANAGEMENT"
        command.cpuSet = cpuSet
        command.memory = memory
        command.handles = handles
        return command
    }

    private static ResourceConsumerHandle systemdHandle(String serviceName, String unit) {
        ResourceConsumerHandle handle = new ResourceConsumerHandle()
        handle.handleType = ResourceConsumerHandle.SYSTEMD_UNIT
        handle.value = unit
        handle.serviceName = serviceName
        return handle
    }

    private static ResourceConsumerHandle restartableSystemdHandle(String serviceName, String unit) {
        ResourceConsumerHandle handle = systemdHandle(serviceName, unit)
        handle.restartable = true
        return handle
    }
}
