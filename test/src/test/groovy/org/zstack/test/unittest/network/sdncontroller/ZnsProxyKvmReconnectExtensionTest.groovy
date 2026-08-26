package org.zstack.test.unittest.network.sdncontroller

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.header.host.CpuArchitecture
import org.zstack.header.host.HostInventory
import org.zstack.kvm.KVMConstant
import org.zstack.sdnController.znsproxy.ZnsProxyKvmReconnectExtension

class ZnsProxyKvmReconnectExtensionTest {
    private boolean originalUnitTestOn

    @Before
    void setUp() {
        originalUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        CoreGlobalProperty.UNIT_TEST_ON = false
    }

    @After
    void tearDown() {
        CoreGlobalProperty.UNIT_TEST_ON = originalUnitTestOn
    }

    @Test
    void testReconnectInstallsProxyOnlyOnX86Host() {
        RecordingExtension extension = new RecordingExtension()

        extension.connectionReestablished(kvmHost("x86-host", CpuArchitecture.x86_64.name()))
        extension.connectionReestablished(kvmHost("arm-host", CpuArchitecture.aarch64.name()))

        assert extension.ensuredHostUuids == ["x86-host"]
    }

    private static HostInventory kvmHost(String uuid, String architecture) {
        HostInventory host = new HostInventory()
        host.uuid = uuid
        host.hypervisorType = KVMConstant.KVM_HYPERVISOR_TYPE
        host.architecture = architecture
        return host
    }

    private static class RecordingExtension extends ZnsProxyKvmReconnectExtension {
        List<String> ensuredHostUuids = []

        @Override
        protected void ensureHost(String hostUuid) {
            ensuredHostUuids.add(hostUuid)
        }
    }
}
